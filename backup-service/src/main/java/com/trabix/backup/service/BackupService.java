package com.trabix.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trabix.backup.dto.BackupDTO;
import com.trabix.backup.entity.Backup;
import com.trabix.backup.entity.EstadoBackup;
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
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * Lista de tablas a incluir en el backup.
     * Orden importante: primero las que no tienen FK, luego las dependientes.
     */
    private static final List<String> TABLAS_BACKUP = Arrays.asList(
            // Usuarios primero (no tiene FK)
            "usuarios",
            // Stock de equipos
            "stock_equipos",
            // Configuración
            "configuracion_costos",
            // Inventario y ventas
            "lotes",
            "tandas",
            "ventas",
            // Equipos
            "asignaciones_equipo",
            "pagos_mensualidad",
            // Documentos
            "documentos",
            // Finanzas
            "fondo_recompensas",
            "movimientos_fondo",
            "costos_produccion",
            // Notificaciones
            "notificaciones",
            // Backups (metadata)
            "backups"
    );

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

    // ==================== CREAR BACKUP ====================

    /**
     * Crea un backup completo del sistema.
     */
    @Transactional
    public BackupDTO.Response crearBackup(BackupDTO.CreateRequest request, Long usuarioId) {
        // Verificar que no haya backup en proceso
        if (repository.existsByEstado(EstadoBackup.EN_PROCESO)) {
            throw new ValidacionNegocioException(
                    "Ya hay un backup en proceso. Espere a que termine antes de crear otro.");
        }

        // Generar nombre único
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String nombre = prefijoBackup + "_" + timestamp;
        String nombreArchivo = nombre + ".zip";
        String rutaCompleta = Paths.get(rutaBackups, nombreArchivo).toString();

        // Crear registro en BD
        Backup backup = Backup.builder()
                .nombre(nombre)
                .archivo(rutaCompleta)
                .estado(EstadoBackup.EN_PROCESO)
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
     * Usa REQUIRES_NEW para que tenga su propia transacción.
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

            log.info("Backup completado: {} - Tamaño: {} - Duración: {}", 
                    backup.getNombre(), backup.getTamanoFormateado(), backup.getDuracionFormateada());

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
        metadata.put("version", "2.0");
        metadata.put("sistema", "TRABIX");
        metadata.put("tablas", TABLAS_BACKUP);
        datos.put("metadata", metadata);

        // Exportar cada tabla
        for (String tabla : TABLAS_BACKUP) {
            try {
                List<Map<String, Object>> registros = jdbcTemplate.queryForList(
                        "SELECT * FROM " + tabla + " ORDER BY id");
                datos.put(tabla, registros);
                
                // Actualizar estadísticas en el backup
                actualizarEstadisticas(backup, tabla, registros.size());
                
                log.debug("Tabla {} exportada: {} registros", tabla, registros.size());
            } catch (Exception e) {
                // Si la tabla no existe, guardar lista vacía
                log.warn("No se pudo exportar tabla {}: {}", tabla, e.getMessage());
                datos.put(tabla, new ArrayList<>());
            }
        }

        log.info("Datos recopilados - Usuarios: {}, Ventas: {}, Lotes: {}, Asignaciones: {}, Documentos: {}", 
                backup.getTotalUsuarios(), backup.getTotalVentas(), backup.getTotalLotes(), 
                backup.getTotalAsignaciones(), backup.getTotalDocumentos());

        return datos;
    }

    private void actualizarEstadisticas(Backup backup, String tabla, int cantidad) {
        switch (tabla) {
            case "usuarios" -> backup.setTotalUsuarios(cantidad);
            case "ventas" -> backup.setTotalVentas(cantidad);
            case "lotes" -> backup.setTotalLotes(cantidad);
            case "tandas" -> backup.setTotalTandas(cantidad);
            case "asignaciones_equipo" -> backup.setTotalAsignaciones(cantidad);
            case "documentos" -> backup.setTotalDocumentos(cantidad);
            case "notificaciones" -> backup.setTotalNotificaciones(cantidad);
        }
    }

    // ==================== CONSULTAS ====================

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
        Page<Backup> page = repository.findByEstado(EstadoBackup.COMPLETADO, pageable);
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
        long completados = repository.countByEstado(EstadoBackup.COMPLETADO);
        long enProceso = repository.countByEstado(EstadoBackup.EN_PROCESO);
        long conError = repository.countByEstado(EstadoBackup.ERROR);
        Long tamanoTotal = repository.sumarTamanoTotal();

        BackupDTO.Response ultimoCompletado = repository
                .findTopByEstadoOrderByFechaInicioDesc(EstadoBackup.COMPLETADO)
                .map(this::mapToResponse)
                .orElse(null);

        return BackupDTO.ResumenResponse.builder()
                .totalBackups(total)
                .backupsCompletados(completados)
                .backupsEnProceso(enProceso)
                .backupsConError(conError)
                .tamanoTotalBytes(tamanoTotal)
                .tamanoTotalFormateado(formatearTamano(tamanoTotal))
                .ultimoBackupCompletado(ultimoCompletado)
                .build();
    }

    @Transactional(readOnly = true)
    public BackupDTO.EstadisticasActuales obtenerEstadisticasActuales() {
        return BackupDTO.EstadisticasActuales.builder()
                .totalUsuarios(contarRegistros("usuarios"))
                .totalVentas(contarRegistros("ventas"))
                .totalLotes(contarRegistros("lotes"))
                .totalTandas(contarRegistros("tandas"))
                .totalAsignaciones(contarRegistros("asignaciones_equipo"))
                .stockDisponible(obtenerStockDisponible())
                .totalDocumentos(contarRegistros("documentos"))
                .totalNotificaciones(contarRegistros("notificaciones"))
                .totalCostosProduccion(contarRegistros("costos_produccion"))
                .totalMovimientosFondo(contarRegistros("movimientos_fondo"))
                .build();
    }

    private int contarRegistros(String tabla) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tabla, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int obtenerStockDisponible() {
        try {
            Integer stock = jdbcTemplate.queryForObject(
                    "SELECT kits_disponibles FROM stock_equipos ORDER BY id LIMIT 1", Integer.class);
            return stock != null ? stock : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== DESCARGAR ====================

    /**
     * Descarga el archivo .zip de un backup.
     */
    @Transactional(readOnly = true)
    public Resource descargarBackup(Long id) {
        Backup backup = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Backup", id));

        if (!backup.estaCompletado()) {
            throw new ValidacionNegocioException(
                    "El backup no está completado. Estado actual: " + backup.getEstado().getNombre());
        }

        try {
            Path path = Paths.get(backup.getArchivo());
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ValidacionNegocioException(
                        "El archivo de backup no existe o no es accesible: " + backup.getArchivo());
            }

            log.info("Descargando backup: {} - {}", backup.getNombre(), backup.getTamanoFormateado());
            return resource;
        } catch (Exception e) {
            throw new ValidacionNegocioException("Error al acceder al archivo: " + e.getMessage());
        }
    }

    // ==================== ELIMINAR ====================

    /**
     * Elimina un backup (registro y archivo).
     */
    @Transactional
    public void eliminar(Long id) {
        Backup backup = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Backup", id));

        if (backup.estaEnProceso()) {
            throw new ValidacionNegocioException("No se puede eliminar un backup en proceso");
        }

        // Eliminar archivo físico
        try {
            Path path = Paths.get(backup.getArchivo());
            boolean eliminado = Files.deleteIfExists(path);
            if (eliminado) {
                log.info("Archivo eliminado: {}", path);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}: {}", backup.getArchivo(), e.getMessage());
        }

        // Eliminar registro
        repository.delete(backup);
        log.info("Backup eliminado: {} (ID: {})", backup.getNombre(), backup.getId());
    }

    // ==================== MÉTODOS PRIVADOS ====================

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
                .estadoDescripcion(b.getEstado().getNombre())
                .fechaInicio(b.getFechaInicio())
                .fechaFin(b.getFechaFin())
                .duracionSegundos(b.getDuracionSegundos())
                .duracionFormateada(b.getDuracionFormateada())
                .totalUsuarios(b.getTotalUsuarios())
                .totalVentas(b.getTotalVentas())
                .totalLotes(b.getTotalLotes())
                .totalTandas(b.getTotalTandas())
                .totalAsignaciones(b.getTotalAsignaciones())
                .totalDocumentos(b.getTotalDocumentos())
                .totalNotificaciones(b.getTotalNotificaciones())
                .notas(b.getNotas())
                .mensajeError(b.getMensajeError())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
