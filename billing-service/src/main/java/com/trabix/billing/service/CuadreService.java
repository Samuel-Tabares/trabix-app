package com.trabix.billing.service;

import com.trabix.billing.dto.*;
import com.trabix.billing.entity.*;
import com.trabix.billing.repository.*;
import com.trabix.common.enums.TipoCuadre;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión de cuadres.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuadreService {

    private final CuadreRepository cuadreRepository;
    private final TandaRepository tandaRepository;
    private final LoteRepository loteRepository;
    private final VentaRepository ventaRepository;
    private final CalculadorCuadreService calculadorService;
    private final WhatsAppTextService whatsAppService;

    @Value("${trabix.trigger-cuadre-porcentaje:20}")
    private int triggerCuadrePorcentaje;

    @Transactional
    public CuadreResponse generarCuadre(Long tandaId, boolean forzar) {
        Tanda tanda = tandaRepository.findById(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));

        if (!"LIBERADA".equals(tanda.getEstado())) {
            throw new ValidacionNegocioException("Solo se puede generar cuadre de tandas liberadas");
        }

        if (cuadreRepository.existsByTandaIdAndEstado(tandaId, "PENDIENTE")) {
            throw new ValidacionNegocioException("Ya existe un cuadre pendiente para esta tanda");
        }

        if (!forzar && !tanda.requiereCuadre(triggerCuadrePorcentaje)) {
            throw new ValidacionNegocioException(
                    String.format("El stock debe estar en %d%% o menos. Actual: %.1f%%",
                            triggerCuadrePorcentaje, tanda.getPorcentajeStockRestante()));
        }

        Lote lote = tanda.getLote();
        TipoCuadre tipoCuadre = tanda.esTandaInversion() ? TipoCuadre.INVERSION : TipoCuadre.GANANCIA;

        CalculoCuadreResponse calculo = calculadorService.calcular(tanda, tipoCuadre);
        String textoWhatsApp = whatsAppService.generarTexto(tanda, calculo);

        Cuadre cuadre = Cuadre.builder()
                .tanda(tanda)
                .tipo(tipoCuadre)
                .totalRecaudado(calculo.getTotalRecaudado())
                .montoEsperado(calculo.getMontoQueDebeTransferir())
                .montoVendedor(calculo.getMontoParaVendedor())
                .montoCascada(tipoCuadre == TipoCuadre.GANANCIA && "MODELO_50_50".equals(lote.getModelo())
                        ? calculo.getDisponibleTotal().subtract(calculo.getMontoParaVendedor()) : null)
                .excedenteAnterior(calculo.getExcedenteAnterior())
                .excedente(calculo.getExcedenteResultante())
                .textoWhatsapp(textoWhatsApp)
                .estado("PENDIENTE")
                .build();

        cuadre = cuadreRepository.save(cuadre);

        tanda.setEstado("EN_CUADRE");
        tandaRepository.save(tanda);

        log.info("🔔 Cuadre generado: ID={}, Tanda={}, Tipo={}, MontoEsperado={}",
                cuadre.getId(), tandaId, tipoCuadre, cuadre.getMontoEsperado());

        return mapToResponse(cuadre);
    }

    @Transactional
    public CuadreResponse confirmarCuadre(Long cuadreId, ConfirmarCuadreRequest request) {
        Cuadre cuadre = cuadreRepository.findById(cuadreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", cuadreId));

        if (!cuadre.estaPendiente() && !"EN_PROCESO".equals(cuadre.getEstado())) {
            throw new ValidacionNegocioException("El cuadre no está pendiente de confirmación");
        }

        if (request.getMontoRecibido().compareTo(cuadre.getMontoEsperado()) < 0) {
            throw new ValidacionNegocioException(
                    String.format("El monto recibido ($%s) es menor al esperado ($%s)",
                            request.getMontoRecibido(), cuadre.getMontoEsperado()));
        }

        cuadre.setMontoRecibido(request.getMontoRecibido());
        cuadre.confirmar();

        if (request.getMontoRecibido().compareTo(cuadre.getMontoEsperado()) > 0) {
            cuadre.setExcedente(request.getMontoRecibido().subtract(cuadre.getMontoEsperado()));
        }

        cuadreRepository.save(cuadre);

        Tanda tanda = cuadre.getTanda();
        tanda.setEstado("CUADRADA");
        tandaRepository.save(tanda);

        liberarSiguienteTandaSiAplica(tanda.getLote());
        verificarLoteCompletado(tanda.getLote());

        log.info("✅ Cuadre confirmado: ID={}, MontoRecibido={}", cuadreId, request.getMontoRecibido());

        return mapToResponse(cuadre);
    }

    private void liberarSiguienteTandaSiAplica(Lote lote) {
        List<Tanda> tandas = tandaRepository.findByLoteIdOrderByNumeroAsc(lote.getId());

        for (Tanda tanda : tandas) {
            if ("PENDIENTE".equals(tanda.getEstado())) {
                if (tanda.getNumero() == 1 || tandaRepository.isTandaCuadrada(lote.getId(), tanda.getNumero() - 1)) {
                    tanda.setEstado("LIBERADA");
                    tanda.setStockEntregado(tanda.getCantidadAsignada());
                    tanda.setStockActual(tanda.getCantidadAsignada());
                    tanda.setFechaLiberacion(LocalDateTime.now());
                    tandaRepository.save(tanda);
                    log.info("📦 Tanda liberada: Lote={}, Tanda={}", lote.getId(), tanda.getNumero());
                }
                break;
            }
        }
    }

    private void verificarLoteCompletado(Lote lote) {
        List<Tanda> tandas = tandaRepository.findByLoteIdOrderByNumeroAsc(lote.getId());
        boolean todasCuadradas = tandas.stream().allMatch(t -> "CUADRADA".equals(t.getEstado()));

        if (todasCuadradas) {
            lote.setEstado("COMPLETADO");
            loteRepository.save(lote);
            log.info("🎉 Lote completado: ID={}", lote.getId());
        }
    }

    @Transactional(readOnly = true)
    public CuadreResponse obtenerCuadre(Long id) {
        Cuadre cuadre = cuadreRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", id));
        return mapToResponse(cuadre);
    }

    @Transactional(readOnly = true)
    public Page<CuadreResponse> listarCuadresPendientes(Pageable pageable) {
        return cuadreRepository.findByEstado("PENDIENTE", pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CuadreResponse> listarCuadresDeUsuario(Long usuarioId) {
        return cuadreRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CuadreResponse> listarCuadresDeLote(Long loteId) {
        return cuadreRepository.findByLoteId(loteId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String obtenerTextoWhatsApp(Long cuadreId) {
        Cuadre cuadre = cuadreRepository.findById(cuadreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", cuadreId));
        return cuadre.getTextoWhatsapp();
    }

    @Transactional(readOnly = true)
    public List<CuadreResponse> detectarTandasParaCuadre() {
        List<Tanda> tandas = tandaRepository.findTandasParaCuadre(triggerCuadrePorcentaje);

        return tandas.stream()
                .filter(t -> !cuadreRepository.existsByTandaIdAndEstado(t.getId(), "PENDIENTE"))
                .map(t -> CuadreResponse.builder()
                        .tanda(CuadreResponse.TandaInfo.builder()
                                .id(t.getId()).numero(t.getNumero())
                                .stockEntregado(t.getStockEntregado())
                                .stockActual(t.getStockActual())
                                .porcentajeRestante(t.getPorcentajeStockRestante())
                                .build())
                        .tipo(t.esTandaInversion() ? TipoCuadre.INVERSION : TipoCuadre.GANANCIA)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumenCuadresResponse obtenerResumen() {
        long pendientes = cuadreRepository.countByEstado("PENDIENTE");
        long enProceso = cuadreRepository.countByEstado("EN_PROCESO");
        long exitosos = cuadreRepository.countByEstado("EXITOSO");

        List<Cuadre> cuadresPendientes = cuadreRepository.findByEstadoOrderByCreatedAtAsc("PENDIENTE");

        List<ResumenCuadresResponse.CuadrePendienteInfo> pendientesInfo = cuadresPendientes.stream()
                .map(c -> {
                    Tanda tanda = c.getTanda();
                    Usuario vendedor = tanda.getLote().getUsuario();
                    Duration tiempo = Duration.between(c.getCreatedAt(), LocalDateTime.now());

                    return ResumenCuadresResponse.CuadrePendienteInfo.builder()
                            .cuadreId(c.getId())
                            .vendedorNombre(vendedor.getNombre())
                            .vendedorTelefono(vendedor.getTelefono())
                            .tandaNumero(tanda.getNumero())
                            .tipoCuadre(c.getTipo().name())
                            .montoEsperado(c.getMontoEsperado())
                            .porcentajeStock(tanda.getPorcentajeStockRestante())
                            .tiempoEspera(formatearTiempo(tiempo))
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalEsperado = cuadresPendientes.stream()
                .map(Cuadre::getMontoEsperado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResumenCuadresResponse.builder()
                .cuadresPendientes((int) pendientes)
                .cuadresEnProceso((int) enProceso)
                .cuadresExitosos((int) exitosos)
                .cuadresTotales((int) (pendientes + enProceso + exitosos))
                .totalEsperado(totalEsperado)
                .pendientes(pendientesInfo)
                .build();
    }

    @Transactional(readOnly = true)
    public CalculoCuadreResponse obtenerDetalleCalculo(Long tandaId) {
        Tanda tanda = tandaRepository.findById(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));
        TipoCuadre tipo = tanda.esTandaInversion() ? TipoCuadre.INVERSION : TipoCuadre.GANANCIA;
        return calculadorService.calcular(tanda, tipo);
    }

    private String formatearTiempo(Duration duracion) {
        long horas = duracion.toHours();
        if (horas < 1) return "Hace " + duracion.toMinutes() + " minutos";
        else if (horas < 24) return "Hace " + horas + " hora" + (horas > 1 ? "s" : "");
        else {
            long dias = duracion.toDays();
            return "Hace " + dias + " día" + (dias > 1 ? "s" : "");
        }
    }

    private CuadreResponse mapToResponse(Cuadre cuadre) {
        Tanda tanda = cuadre.getTanda();
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();

        return CuadreResponse.builder()
                .id(cuadre.getId())
                .tanda(CuadreResponse.TandaInfo.builder()
                        .id(tanda.getId()).numero(tanda.getNumero())
                        .descripcion(getTandaDescripcion(tanda))
                        .stockEntregado(tanda.getStockEntregado())
                        .stockActual(tanda.getStockActual())
                        .porcentajeRestante(tanda.getPorcentajeStockRestante())
                        .build())
                .lote(CuadreResponse.LoteInfo.builder()
                        .id(lote.getId()).cantidadTotal(lote.getCantidadTotal())
                        .modelo(lote.getModelo())
                        .inversionTotal(lote.getInversionPercibidaTotal())
                        .build())
                .vendedor(CuadreResponse.VendedorInfo.builder()
                        .id(vendedor.getId()).nombre(vendedor.getNombre())
                        .cedula(vendedor.getCedula()).nivel(vendedor.getNivel())
                        .telefono(vendedor.getTelefono())
                        .build())
                .tipo(cuadre.getTipo()).estado(cuadre.getEstado())
                .totalRecaudado(cuadre.getTotalRecaudado())
                .montoEsperado(cuadre.getMontoEsperado())
                .montoRecibido(cuadre.getMontoRecibido())
                .excedente(cuadre.getExcedente())
                .excedenteAnterior(cuadre.getExcedenteAnterior())
                .montoVendedor(cuadre.getMontoVendedor())
                .montoCascada(cuadre.getMontoCascada())
                .textoWhatsapp(cuadre.getTextoWhatsapp())
                .fechaCreacion(cuadre.getCreatedAt())
                .fechaConfirmacion(cuadre.getFecha())
                .build();
    }

    private String getTandaDescripcion(Tanda tanda) {
        return switch (tanda.getNumero()) {
            case 1 -> "Tanda 1 (Inversión)";
            case 2 -> "Tanda 2 (Recuperación + Ganancia)";
            case 3 -> "Tanda 3 (Ganancia Pura)";
            default -> "Tanda " + tanda.getNumero();
        };
    }
}
