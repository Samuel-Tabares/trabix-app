package com.trabix.equipment.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.equipment.dto.EquipoDTO;
import com.trabix.equipment.entity.Equipo;
import com.trabix.equipment.entity.PagoMensualidad;
import com.trabix.equipment.entity.Usuario;
import com.trabix.equipment.repository.EquipoRepository;
import com.trabix.equipment.repository.PagoMensualidadRepository;
import com.trabix.equipment.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de equipos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final PagoMensualidadRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${trabix.equipos.costo-reposicion.nevera:350000}")
    private BigDecimal costoReposicionNevera;

    @Value("${trabix.equipos.costo-reposicion.pijama:80000}")
    private BigDecimal costoReposicionPijama;

    @Value("${trabix.equipos.mensualidad-default:10000}")
    private BigDecimal mensualidadDefault;

    @Transactional
    public EquipoDTO.Response asignar(EquipoDTO.CreateRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getUsuarioId()));

        // Verificar si ya tiene un equipo activo del mismo tipo
        if (equipoRepository.existsByUsuarioIdAndTipoAndEstado(
                request.getUsuarioId(), request.getTipo(), "ACTIVO")) {
            throw new ValidacionNegocioException(
                    "El usuario ya tiene un(a) " + request.getTipo() + " activo(a)");
        }

        // Determinar costo de reposición
        BigDecimal costoReposicion = request.getCostoReposicion();
        if (costoReposicion == null) {
            costoReposicion = "NEVERA".equals(request.getTipo()) ? 
                    costoReposicionNevera : costoReposicionPijama;
        }

        Equipo equipo = Equipo.builder()
                .usuario(usuario)
                .tipo(request.getTipo())
                .fechaInicio(LocalDateTime.now())
                .estado("ACTIVO")
                .costoReposicion(costoReposicion)
                .numeroSerie(request.getNumeroSerie())
                .descripcion(request.getDescripcion())
                .build();

        Equipo saved = equipoRepository.save(equipo);

        // Crear mensualidad del mes actual
        crearMensualidadActual(saved);

        log.info("Equipo asignado: {} {} a {} ({})", 
                saved.getTipo(), saved.getId(), usuario.getNombre(), usuario.getCedula());

        return mapToResponse(saved);
    }

    @Transactional
    public EquipoDTO.Response actualizar(Long id, EquipoDTO.UpdateRequest request) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (request.getNumeroSerie() != null) {
            equipo.setNumeroSerie(request.getNumeroSerie());
        }
        if (request.getDescripcion() != null) {
            equipo.setDescripcion(request.getDescripcion());
        }
        if (request.getCostoReposicion() != null) {
            equipo.setCostoReposicion(request.getCostoReposicion());
        }

        Equipo saved = equipoRepository.save(equipo);
        log.info("Equipo actualizado: {} (ID: {})", saved.getTipo(), saved.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public EquipoDTO.Response devolver(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (!equipo.estaActivo()) {
            throw new ValidacionNegocioException("El equipo no está activo");
        }

        // Verificar pagos pendientes
        List<PagoMensualidad> pendientes = pagoRepository
                .findByEquipoIdAndEstadoOrderByAnioAscMesAsc(id, "PENDIENTE");
        
        if (!pendientes.isEmpty()) {
            throw new ValidacionNegocioException(
                    "El equipo tiene " + pendientes.size() + " pago(s) pendiente(s). " +
                    "Debe estar al día para devolver.");
        }

        equipo.devolver();
        Equipo saved = equipoRepository.save(equipo);

        log.info("Equipo devuelto: {} (ID: {}) por {} ({})", 
                saved.getTipo(), saved.getId(), 
                saved.getUsuario().getNombre(), saved.getUsuario().getCedula());

        return mapToResponse(saved);
    }

    @Transactional
    public EquipoDTO.Response marcarPerdido(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (!equipo.estaActivo()) {
            throw new ValidacionNegocioException("El equipo no está activo");
        }

        equipo.marcarPerdido();
        Equipo saved = equipoRepository.save(equipo);

        log.warn("Equipo marcado como PERDIDO: {} (ID: {}) - Usuario: {} ({}) - Costo reposición: ${}",
                saved.getTipo(), saved.getId(),
                saved.getUsuario().getNombre(), saved.getUsuario().getCedula(),
                saved.getCostoReposicion());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public EquipoDTO.Response obtener(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
        return mapToResponse(equipo);
    }

    @Transactional(readOnly = true)
    public EquipoDTO.ListResponse listar(Pageable pageable) {
        Page<Equipo> page = equipoRepository.findAll(pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public EquipoDTO.ListResponse listarPorEstado(String estado, Pageable pageable) {
        Page<Equipo> page = equipoRepository.findByEstado(estado.toUpperCase(), pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public EquipoDTO.ListResponse listarPorTipo(String tipo, Pageable pageable) {
        Page<Equipo> page = equipoRepository.findByTipo(tipo.toUpperCase(), pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<EquipoDTO.Response> listarPorUsuario(Long usuarioId) {
        return equipoRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipoDTO.Response> listarEquiposActivosUsuario(Long usuarioId) {
        return equipoRepository.findEquiposActivosByUsuario(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EquipoDTO.ResumenUsuario obtenerResumenUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        List<Equipo> equiposActivos = equipoRepository.findByUsuarioIdAndEstado(usuarioId, "ACTIVO");
        long neveras = equiposActivos.stream().filter(Equipo::esNevera).count();
        long pijamas = equiposActivos.stream().filter(Equipo::esPijama).count();

        long pagosPendientes = pagoRepository.countPagosPendientesByUsuario(usuarioId);
        BigDecimal totalPendiente = pagoRepository.sumarMontoPendienteByUsuario(usuarioId);

        return EquipoDTO.ResumenUsuario.builder()
                .usuarioId(usuarioId)
                .nombre(usuario.getNombre())
                .cedula(usuario.getCedula())
                .equiposActivos(equiposActivos.size())
                .neveras((int) neveras)
                .pijamas((int) pijamas)
                .pagosPendientes((int) pagosPendientes)
                .totalPendiente(totalPendiente)
                .build();
    }

    @Transactional(readOnly = true)
    public EquipoDTO.ResumenGeneral obtenerResumenGeneral() {
        long totalEquipos = equipoRepository.count();
        long activos = equipoRepository.countByEstado("ACTIVO");
        long devueltos = equipoRepository.countByEstado("DEVUELTO");
        long perdidos = equipoRepository.countByEstado("PERDIDO");
        long neveras = equipoRepository.countByTipo("NEVERA");
        long pijamas = equipoRepository.countByTipo("PIJAMA");

        long pagosPendientes = pagoRepository.countByEstado("PENDIENTE");
        BigDecimal montoPendiente = pagoRepository.sumarTotalPendiente();
        BigDecimal montoPagado = pagoRepository.sumarTotalPagado();

        return EquipoDTO.ResumenGeneral.builder()
                .totalEquipos(totalEquipos)
                .equiposActivos(activos)
                .equiposDevueltos(devueltos)
                .equiposPerdidos(perdidos)
                .totalNeveras(neveras)
                .totalPijamas(pijamas)
                .pagosPendientes(pagosPendientes)
                .montoPendiente(montoPendiente)
                .montoPagado(montoPagado)
                .build();
    }

    /**
     * Crea la mensualidad del mes actual para un equipo.
     */
    private void crearMensualidadActual(Equipo equipo) {
        LocalDateTime ahora = LocalDateTime.now();
        int mes = ahora.getMonthValue();
        int anio = ahora.getYear();

        if (!pagoRepository.existsByEquipoIdAndMesAndAnio(equipo.getId(), mes, anio)) {
            PagoMensualidad pago = PagoMensualidad.builder()
                    .equipo(equipo)
                    .mes(mes)
                    .anio(anio)
                    .monto(mensualidadDefault)
                    .estado("PENDIENTE")
                    .build();
            pagoRepository.save(pago);
            log.debug("Mensualidad creada: {} {} para equipo {}", mes, anio, equipo.getId());
        }
    }

    private EquipoDTO.ListResponse buildListResponse(Page<Equipo> page) {
        List<EquipoDTO.Response> equipos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return EquipoDTO.ListResponse.builder()
                .equipos(equipos)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }

    private EquipoDTO.Response mapToResponse(Equipo equipo) {
        List<PagoMensualidad> pendientes = pagoRepository
                .findByEquipoIdAndEstadoOrderByAnioAscMesAsc(equipo.getId(), "PENDIENTE");
        
        BigDecimal montoPendiente = pendientes.stream()
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EquipoDTO.Response.builder()
                .id(equipo.getId())
                .usuarioId(equipo.getUsuario().getId())
                .usuarioNombre(equipo.getUsuario().getNombre())
                .usuarioCedula(equipo.getUsuario().getCedula())
                .tipo(equipo.getTipo())
                .fechaInicio(equipo.getFechaInicio())
                .estado(equipo.getEstado())
                .costoReposicion(equipo.getCostoReposicion())
                .numeroSerie(equipo.getNumeroSerie())
                .descripcion(equipo.getDescripcion())
                .fechaDevolucion(equipo.getFechaDevolucion())
                .pagosPendientes(pendientes.size())
                .montoPendiente(montoPendiente)
                .createdAt(equipo.getCreatedAt())
                .build();
    }
}
