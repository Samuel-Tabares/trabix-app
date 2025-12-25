package com.trabix.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trabix.backup.dto.BackupDTO;
import com.trabix.backup.entity.Backup;
import com.trabix.backup.repository.BackupRepository;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio para crear y gestionar backups COMPLETOS del sistema.
 * 
 * Los backups se guardan como archivos .zip en la ruta configurada.
 * NUNCA se borran automáticamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${trabix.backup.ruta:./backups}")
    private String rutaBackups;

    @Value("${trabix.backup.prefijo:backup_TRABIX}")
    private String prefijoBackup;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // Configurar ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Crear directorio de backups si no existe
        try {
            Path path = Paths.get(rutaBackups);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Directorio de backups creado: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Error al crear directorio de backups: {}", e.getMessage());
        }
    }

    /**
     * Crea un backup completo del sistema.
     */
    @Transactional
    public BackupDTO.Response crearBackup(BackupDTO.CreateRequest request, Long usuarioId) {
        // Generar nombre único
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String nombre = prefijoBackup + "_" + timestamp;
        String nombreArchivo = nombre + ".zip";
        String rutaCompleta = Paths.get(rutaBackups, nombreArchivo).toString();

        // Crear registro en BD
        Backup backup = Backup.builder()
                .nombre(nombre)
                .archivo(rutaCompleta)
                .estado("EN_PROCESO")
                .fechaInicio(LocalDateTime.now())
                .notas(request != null ? request.getNotas() : null)
                .createdBy(usuarioId)
                .build();

        Backup saved = repository.save(backup);
        log.info("Iniciando backup: {} (ID: {})", nombre, saved.getId());

        // Ejecutar backup en segundo plano
        ejecutarBackupAsync(saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Ejecuta el backup de forma asíncrona.
     */
    @Async
    @Transactional
    public void ejecutarBackupAsync(Long backupId) {
        Backup backup = repository.findById(backupId).orElse(null);
        if (backup == null) {
            log.error("Backup no encontrado: {}", backupId);
            return;
        }

        try {
            // Recopilar todos los datos
            Map<String, Object> datos = recopilarDatos(backup);

            // Crear archivo JSON
            String jsonContent = objectMapper.writeValueAsString(datos);

            // Crear archivo ZIP
            Path zipPath = Paths.get(backup.getArchivo());
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                ZipEntry entry = new ZipEntry(backup.getNombre() + ".json");
                zos.putNextEntry(entry);
                zos.write(jsonContent.getBytes("UTF-8"));
                zos.closeEntry();
            }

            // Actualizar registro
            long tamano = Files.size(zipPath);
            backup.completar(tamano);
            repository.save(backup);

            log.info("Backup completado: {} - Tamaño: {} - Archivo: {}", 
                    backup.getNombre(), backup.getTamanoFormateado(), backup.getArchivo());

        } catch (Exception e) {
            log.error("Error al crear backup {}: {}", backup.getNombre(), e.getMessage(), e);
            backup.marcarError("Error: " + e.getMessage());
            repository.save(backup);
        }
    }

    /**
     * Recopila todos los datos del sistema para el backup.
     */
    private Map<String, Object> recopilarDatos(Backup backup) {
        Map<String, Object> datos = new LinkedHashMap<>();

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", backup.getId());
        metadata.put("nombre", backup.getNombre());
        metadata.put("fechaCreacion", LocalDateTime.now().toString());
        metadata.put("version", "1.0");
        metadata.put("sistema", "TRABIX");
        datos.put("metadata", metadata);

        // Usuarios
        List<Map<String, Object>> usuarios = jdbcTemplate.queryForList(
                "SELECT * FROM usuarios ORDER BY id");
        backup.setTotalUsuarios(usuarios.size());
        datos.put("usuarios", usuarios);

        // Ventas
        List<Map<String, Object>> ventas = jdbcTemplate.queryForList(
                "SELECT * FROM ventas ORDER BY id");
        backup.setTotalVentas(ventas.size());
        datos.put("ventas", ventas);

        // Lotes
        List<Map<String, Object>> lotes = jdbcTemplate.queryForList(
                "SELECT * FROM lotes ORDER BY id");
        backup.setTotalLotes(lotes.size());
        datos.put("lotes", lotes);

        // Tandas
        List<Map<String, Object>> tandas = jdbcTemplate.queryForList(
                "SELECT * FROM tandas ORDER BY id");
        backup.setTotalTandas(tandas.size());
        datos.put("tandas", tandas);

        // Equipos
        List<Map<String, Object>> equipos = jdbcTemplate.queryForList(
                "SELECT * FROM equipos ORDER BY id");
        backup.setTotalEquipos(equipos.size());
        datos.put("equipos", equipos);

        // Pagos de mensualidad
        List<Map<String, Object>> pagosMensualidad = jdbcTemplate.queryForList(
                "SELECT * FROM pagos_mensualidad ORDER BY id");
        datos.put("pagosMensualidad", pagosMensualidad);

        // Documentos
        List<Map<String, Object>> documentos = jdbcTemplate.queryForList(
                "SELECT * FROM documentos ORDER BY id");
        backup.setTotalDocumentos(documentos.size());
        datos.put("documentos", documentos);

        // Notificaciones
        List<Map<String, Object>> notificaciones = jdbcTemplate.queryForList(
                "SELECT * FROM notificaciones ORDER BY id");
        backup.setTotalNotificaciones(notificaciones.size());
        datos.put("notificaciones", notificaciones);

        // Configuración de costos
        List<Map<String, Object>> configuracionCostos = jdbcTemplate.queryForList(
                "SELECT * FROM configuracion_costos ORDER BY id");
        datos.put("configuracionCostos", configuracionCostos);

        // Fondo de recompensas
        List<Map<String, Object>> fondoRecompensas = jdbcTemplate.queryForList(
                "SELECT * FROM fondo_recompensas ORDER BY id");
        datos.put("fondoRecompensas", fondoRecompensas);

        // Movimientos del fondo
        List<Map<String, Object>> movimientosFondo = jdbcTemplate.queryForList(
                "SELECT * FROM movimientos_fondo ORDER BY id");
        datos.put("movimientosFondo", movimientosFondo);

        // Costos de producción
        List<Map<String, Object>> costosProduccion = jdbcTemplate.queryForList(
                "SELECT * FROM costos_produccion ORDER BY id");
        datos.put("costosProduccion", costosProduccion);

        // Cuadres
        try {
            List<Map<String, Object>> cuadres = jdbcTemplate.queryForList(
                    "SELECT * FROM cuadres ORDER BY id");
            datos.put("cuadres", cuadres);
        } catch (Exception e) {
            // Tabla puede no existir
            datos.put("cuadres", new ArrayList<>());
        }

        log.info("Datos recopilados - Usuarios: {}, Ventas: {}, Lotes: {}, Tandas: {}, Equipos: {}, Documentos: {}", 
                usuarios.size(), ventas.size(), lotes.size(), tandas.size(), equipos.size(), documentos.size());

        return datos;
    }

    @Transactional(readOnly = true)
    public BackupDTO.Response obtener(Long id) {
        Backup backup = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Backup", id));
        return mapToResponse(backup);
    }

    @Transactional(readOnly = true)
    public BackupDTO.ListResponse listar(Pageable pageable) {
        Page<Backup> page = repository.findAll(pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public BackupDTO.ListResponse listarCompletados(Pageable pageable) {
        Page<Backup> page = repository.findByEstado("COMPLETADO", pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<BackupDTO.Response> listarRecientes() {
        return repository.findTop10ByOrderByFechaInicioDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BackupDTO.ResumenResponse obtenerResumen() {
        long total = repository.count();
        long completados = repository.countByEstado("COMPLETADO");
        long conError = repository.countByEstado("ERROR");
        Long tamanoTotal = repository.sumarTamanoTotal();

        BackupDTO.Response ultimoBackup = repository.findTopByOrderByFechaInicioDesc()
                .map(this::mapToResponse)
                .orElse(null);

        return BackupDTO.ResumenResponse.builder()
                .totalBackups(total)
                .backupsCompletados(completados)
                .backupsConError(conError)
                .tamanoTotalBytes(tamanoTotal)
                .tamanoTotalFormateado(formatearTamano(tamanoTotal))
                .ultimoBackup(ultimoBackup)
                .build();
    }

    /**
     * Descarga el archivo .zip de un backup.
     */
    @Transactional(readOnly = true)
    public Resource descargarBackup(Long id) {
        Backup backup = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Backup", id));

        if (!backup.estaCompletado()) {
            throw new ValidacionNegocioException("El backup no está completado");
        }

        try {
            Path path = Paths.get(backup.getArchivo());
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ValidacionNegocioException("El archivo de backup no existe o no es accesible");
            }

            return resource;
        } catch (Exception e) {
            throw new ValidacionNegocioException("Error al acceder al archivo: " + e.getMessage());
        }
    }

    /**
     * Elimina un backup (registro y archivo).
     */
    @Transactional
    public void eliminar(Long id) {
        Backup backup = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Backup", id));

        // Eliminar archivo físico
        try {
            Path path = Paths.get(backup.getArchivo());
            Files.deleteIfExists(path);
            log.info("Archivo eliminado: {}", path);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo: {}", e.getMessage());
        }

        // Eliminar registro
        repository.delete(backup);
        log.info("Backup eliminado: {} (ID: {})", backup.getNombre(), backup.getId());
    }

    @Transactional(readOnly = true)
    public BackupDTO.EstadisticasBackup obtenerEstadisticasActuales() {
        return BackupDTO.EstadisticasBackup.builder()
                .totalUsuarios(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class))
                .totalVentas(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class))
                .totalLotes(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lotes", Integer.class))
                .totalTandas(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tandas", Integer.class))
                .totalEquipos(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM equipos", Integer.class))
                .totalDocumentos(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documentos", Integer.class))
                .totalNotificaciones(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notificaciones", Integer.class))
                .build();
    }

    private String formatearTamano(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        String[] unidades = {"B", "KB", "MB", "GB"};
        int indice = 0;
        double tamano = bytes;

        while (tamano >= 1024 && indice < unidades.length - 1) {
            tamano /= 1024;
            indice++;
        }

        return String.format("%.2f %s", tamano, unidades[indice]);
    }

    private BackupDTO.ListResponse buildListResponse(Page<Backup> page) {
        List<BackupDTO.Response> backups = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return BackupDTO.ListResponse.builder()
                .backups(backups)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }

    private BackupDTO.Response mapToResponse(Backup b) {
        return BackupDTO.Response.builder()
                .id(b.getId())
                .nombre(b.getNombre())
                .archivo(b.getArchivo())
                .tamanoBytes(b.getTamanoBytes())
                .tamanoFormateado(b.getTamanoFormateado())
                .estado(b.getEstado())
                .fechaInicio(b.getFechaInicio())
                .fechaFin(b.getFechaFin())
                .duracionSegundos(b.getDuracionSegundos())
                .totalUsuarios(b.getTotalUsuarios())
                .totalVentas(b.getTotalVentas())
                .totalLotes(b.getTotalLotes())
                .totalTandas(b.getTotalTandas())
                .totalEquipos(b.getTotalEquipos())
                .totalDocumentos(b.getTotalDocumentos())
                .totalNotificaciones(b.getTotalNotificaciones())
                .notas(b.getNotas())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
