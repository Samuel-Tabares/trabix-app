package com.trabix.equipment.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.equipment.dto.PagoMensualidadDTO;
import com.trabix.equipment.entity.Equipo;
import com.trabix.equipment.entity.PagoMensualidad;
import com.trabix.equipment.repository.EquipoRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de pagos de mensualidad.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoMensualidadService {

    private final PagoMensualidadRepository pagoRepository;
    private final EquipoRepository equipoRepository;

    @Value("${trabix.equipos.mensualidad-default:10000}")
    private BigDecimal mensualidadDefault;

    @Transactional
    public PagoMensualidadDTO.Response registrarPago(Long pagoId, PagoMensualidadDTO.RegistrarPagoRequest request) {
        PagoMensualidad pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", pagoId));

        if (pago.estaPagado()) {
            throw new ValidacionNegocioException("Este pago ya fue registrado");
        }

        pago.marcarPagado();
        if (request != null && request.getNota() != null) {
            pago.setNota(request.getNota());
        }

        PagoMensualidad saved = pagoRepository.save(pago);

        log.info("Pago registrado: {} {} - Equipo {} ({}) - Usuario: {} ({})",
                pago.getNombreMes(), pago.getAnio(),
                pago.getEquipo().getTipo(), pago.getEquipo().getId(),
                pago.getEquipo().getUsuario().getNombre(),
                pago.getEquipo().getUsuario().getCedula());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.Response obtener(Long id) {
        PagoMensualidad pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
        return mapToResponse(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarPorEquipo(Long equipoId) {
        return pagoRepository.findByEquipoIdOrderByAnioDescMesDesc(equipoId).stream()
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
    public PagoMensualidadDTO.ListResponse listarPendientes(Pageable pageable) {
        Page<PagoMensualidad> page = pagoRepository.findByEstado("PENDIENTE", pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ListResponse listarPagados(Pageable pageable) {
        Page<PagoMensualidad> page = pagoRepository.findByEstado("PAGADO", pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<PagoMensualidadDTO.Response> listarPorMes(Integer mes, Integer anio) {
        return pagoRepository.findByMesYAnio(mes, anio).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagoMensualidadDTO.ResumenMes obtenerResumenMes(Integer mes, Integer anio) {
        List<PagoMensualidad> pagos = pagoRepository.findByMesYAnio(mes, anio);
        
        long pagados = pagos.stream().filter(PagoMensualidad::estaPagado).count();
        long pendientes = pagos.stream().filter(PagoMensualidad::estaPendiente).count();
        
        BigDecimal montoPagado = pagos.stream()
                .filter(PagoMensualidad::estaPagado)
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal montoPendiente = pagos.stream()
                .filter(PagoMensualidad::estaPendiente)
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
                .montoPagado(montoPagado)
                .montoPendiente(montoPendiente)
                .build();
    }

    /**
     * Genera mensualidades del mes actual para todos los equipos activos.
     * Se ejecuta el día 1 de cada mes a las 00:00.
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generarMensualidadesMensuales() {
        log.info("Iniciando generación de mensualidades mensuales...");
        
        LocalDateTime ahora = LocalDateTime.now();
        int mes = ahora.getMonthValue();
        int anio = ahora.getYear();

        List<Equipo> equiposActivos = equipoRepository.findByEstado("ACTIVO", Pageable.unpaged()).getContent();
        int generadas = 0;

        for (Equipo equipo : equiposActivos) {
            if (!pagoRepository.existsByEquipoIdAndMesAndAnio(equipo.getId(), mes, anio)) {
                PagoMensualidad pago = PagoMensualidad.builder()
                        .equipo(equipo)
                        .mes(mes)
                        .anio(anio)
                        .monto(mensualidadDefault)
                        .estado("PENDIENTE")
                        .build();
                pagoRepository.save(pago);
                generadas++;
            }
        }

        log.info("Mensualidades generadas: {} de {} equipos activos", generadas, equiposActivos.size());
    }

    /**
     * Genera mensualidades manualmente (para admin).
     */
    @Transactional
    public int generarMensualidades(Integer mes, Integer anio) {
        List<Equipo> equiposActivos = equipoRepository.findByEstado("ACTIVO", Pageable.unpaged()).getContent();
        int generadas = 0;

        for (Equipo equipo : equiposActivos) {
            if (!pagoRepository.existsByEquipoIdAndMesAndAnio(equipo.getId(), mes, anio)) {
                PagoMensualidad pago = PagoMensualidad.builder()
                        .equipo(equipo)
                        .mes(mes)
                        .anio(anio)
                        .monto(mensualidadDefault)
                        .estado("PENDIENTE")
                        .build();
                pagoRepository.save(pago);
                generadas++;
            }
        }

        log.info("Mensualidades generadas manualmente: {} para {}/{}", generadas, mes, anio);
        return generadas;
    }

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

    private PagoMensualidadDTO.Response mapToResponse(PagoMensualidad pago) {
        return PagoMensualidadDTO.Response.builder()
                .id(pago.getId())
                .equipoId(pago.getEquipo().getId())
                .tipoEquipo(pago.getEquipo().getTipo())
                .usuarioId(pago.getEquipo().getUsuario().getId())
                .usuarioNombre(pago.getEquipo().getUsuario().getNombre())
                .usuarioCedula(pago.getEquipo().getUsuario().getCedula())
                .mes(pago.getMes())
                .anio(pago.getAnio())
                .periodo(pago.getPeriodo())
                .monto(pago.getMonto())
                .fechaPago(pago.getFechaPago())
                .estado(pago.getEstado())
                .nota(pago.getNota())
                .build();
    }
}
