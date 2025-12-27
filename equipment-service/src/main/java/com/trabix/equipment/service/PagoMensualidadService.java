package com.trabix.equipment.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.equipment.dto.PagoMensualidadDTO;
import com.trabix.equipment.entity.AsignacionEquipo;
import com.trabix.equipment.entity.EstadoAsignacion;
import com.trabix.equipment.entity.EstadoPago;
import com.trabix.equipment.entity.PagoMensualidad;
import com.trabix.equipment.repository.AsignacionEquipoRepository;
import com.trabix.equipment.repository.PagoMensualidadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de pagos de mensualidad.
 * 
 * REGLAS:
 * - Mensualidad: $10,000/mes por el kit completo
 * - Fecha de vencimiento: día de cobro mensual de la asignación
 * - Pagos pendientes bloquean cuadres del vendedor
 * - Los pagos vencidos se marcan automáticamente
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoMensualidadService {

    private final PagoMensualidadRepository pagoRepository;
    private final AsignacionEquipoRepository asignacionRepository;

    @Value("${trabix.equipos.mensualidad:10000}")
    private BigDecimal mensualidad;

    // ==================== REGISTRAR PAGO ====================

    @Transactional
    public PagoMensualidadDTO.Response registrarPago(Long pagoId, PagoMensualidadDTO.RegistrarPagoRequest request) {
        PagoMensualidad pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", pagoId));

        if (pago.estaPagado()) {
            throw new ValidacionNegocioException("Este pago ya fue registrado");
        }

        String nota = request != null && request.getNota() != null ? request.getNota() : null;
        pago.marcarPagado(nota);

        PagoMensualidad saved = pagoRepository.save(pago);

        log.info("Pago registrado: {} - {} ({}) - ${}",
                pago.getPeriodo(),
                pago.getAsignacion().getUsuario().getNombre(),
                pago.getAsignacion().getUsuario().getCedula(),
                pago.getMonto());

        return mapToResponse(saved);
    }

    // ==================== GENERAR MENSUALIDADES ====================

    /**
     * Genera mensualidades del mes actual para todas las asignaciones activas.
     * Se ejecuta el día 1 de cada mes a las 00:00.
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generarMensualidadesMensuales() {
        log.info("Iniciando generación de mensualidades mensuales...");
        
        LocalDateTime ahora = LocalDateTime.now();
        int mes = ahora.getMonthValue();
        int anio = ahora.getYear();

        int generadas = generarMensualidades(mes, anio);
        log.info("Mensualidades generadas automáticamente: {} para {}/{}", generadas, mes, anio);
    }

    /**
     * Genera mensualidades manualmente (para admin).
     */
    @Transactional
    public int generarMensualidades(Integer mes, Integer anio) {
        // Validar mes/año
        if (mes < 1 || mes > 12) {
            throw new ValidacionNegocioException("Mes inválido: " + mes);
        }
        if (anio < 2020 || anio > 2100) {
            throw new ValidacionNegocioException("Año inválido: " + anio);
        }

        List<AsignacionEquipo> asignacionesActivas = 
                asignacionRepository.findByEstadoOrderByFechaInicioDesc(EstadoAsignacion.ACTIVO);
        
        int generadas = 0;

        for (AsignacionEquipo asignacion : asignacionesActivas) {
            // Solo generar si no existe ya
            if (!pagoRepository.existsByAsignacionIdAndMesAndAnio(asignacion.getId(), mes, anio)) {
                // Calcular fecha de vencimiento
                int diaVencimiento = Math.min(asignacion.getDiaCobroMensual(), 
                        LocalDate.of(anio, mes, 1).lengthOfMonth());

                PagoMensualidad pago = PagoMensualidad.builder()
                        .asignacion(asignacion)
                        .mes(mes)
                        .anio(anio)
                        .monto(mensualidad)
                        .fechaVencimiento(LocalDate.of(anio, mes, diaVencimiento))
                        .estado(EstadoPago.PENDIENTE)
                        .build();
                
                pagoRepository.save(pago);
                generadas++;
            }
        }

        log.info("Mensualidades generadas: {} para {}/{}", generadas, mes, anio);
        return generadas;
    }

    // ==================== MARCAR VENCIDOS ====================

    /**
     * Marca pagos vencidos automáticamente.
     * Se ejecuta todos los días a las 00:05.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void marcarPagosVencidos() {
        int marcados = pagoRepository.marcarPagosVencidos(LocalDate.now());
        if (marcados > 0) {
            log.info("Pagos marcados como vencidos: {}", marcados);
        }
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.Response obtener(Long id) {
        PagoMensualidad pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
        return mapToResponse(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarPorAsignacion(Long asignacionId) {
        return pagoRepository.findByAsignacionIdOrderByAnioDescMesDesc(asignacionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarPendientesPorUsuario(Long usuarioId) {
        return pagoRepository.findPagosPendientesByUsuario(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarVencidosPorUsuario(Long usuarioId) {
        return pagoRepository.findPagosVencidosByUsuario(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ListResponse listarPendientes(Pageable pageable) {
        Page<PagoMensualidad> page = pagoRepository.findByEstado(EstadoPago.PENDIENTE, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ListResponse listarVencidos(Pageable pageable) {
        Page<PagoMensualidad> page = pagoRepository.findByEstado(EstadoPago.VENCIDO, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ListResponse listarPagados(Pageable pageable) {
        Page<PagoMensualidad> page = pagoRepository.findByEstado(EstadoPago.PAGADO, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarPorMes(Integer mes, Integer anio) {
        return pagoRepository.findByMesYAnio(mes, anio).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== RESUMEN ====================

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ResumenMes obtenerResumenMes(Integer mes, Integer anio) {
        List<PagoMensualidad> pagos = pagoRepository.findByMesYAnio(mes, anio);
        
        long pagados = pagos.stream().filter(PagoMensualidad::estaPagado).count();
        long pendientes = pagos.stream().filter(PagoMensualidad::estaPendiente).count();
        long vencidos = pagos.stream().filter(PagoMensualidad::estaVencido).count();
        
        BigDecimal montoPagado = pagos.stream()
                .filter(PagoMensualidad::estaPagado)
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal montoPendiente = pagos.stream()
                .filter(PagoMensualidad::estaPendiente)
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoVencido = pagos.stream()
                .filter(PagoMensualidad::estaVencido)
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String[] meses = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

        return PagoMensualidadDTO.ResumenMes.builder()
                .mes(mes)
                .anio(anio)
                .periodo(meses[mes] + " " + anio)
                .totalPagos(pagos.size())
                .pagados(pagados)
                .pendientes(pendientes)
                .vencidos(vencidos)
                .montoPagado(montoPagado)
                .montoPendiente(montoPendiente)
                .montoVencido(montoVencido)
                .build();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private PagoMensualidadDTO.ListResponse buildListResponse(Page<PagoMensualidad> page) {
        List<PagoMensualidadDTO.Response> pagos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        BigDecimal totalMonto = page.getContent().stream()
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PagoMensualidadDTO.ListResponse.builder()
                .pagos(pagos)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .totalMonto(totalMonto)
                .build();
    }

    private PagoMensualidadDTO.Response mapToResponse(PagoMensualidad p) {
        boolean vencido = p.estaVencido() || p.deberiaMarcarseVencido();
        int diasVencido = 0;
        
        if (vencido && p.getFechaVencimiento() != null) {
            diasVencido = (int) ChronoUnit.DAYS.between(p.getFechaVencimiento(), LocalDate.now());
            if (diasVencido < 0) diasVencido = 0;
        }

        return PagoMensualidadDTO.Response.builder()
                .id(p.getId())
                .asignacionId(p.getAsignacion().getId())
                .usuarioId(p.getAsignacion().getUsuario().getId())
                .usuarioNombre(p.getAsignacion().getUsuario().getNombre())
                .usuarioCedula(p.getAsignacion().getUsuario().getCedula())
                .mes(p.getMes())
                .anio(p.getAnio())
                .periodo(p.getPeriodo())
                .monto(p.getMonto())
                .fechaVencimiento(p.getFechaVencimiento())
                .fechaPago(p.getFechaPago())
                .estado(p.getEstado())
                .estadoDescripcion(p.getEstado().getNombre())
                .nota(p.getNota())
                .vencido(vencido)
                .diasVencido(diasVencido)
                .build();
    }
}
