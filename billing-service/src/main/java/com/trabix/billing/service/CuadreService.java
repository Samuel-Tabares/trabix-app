package com.trabix.billing.service;

import com.trabix.billing.dto.*;
import com.trabix.billing.entity.*;
import com.trabix.billing.repository.*;
import com.trabix.common.enums.TipoCuadre;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión de cuadres.
 * 
 * LÓGICA DE CUADRES:
 * 
 * TANDA 1 (Inversión Samuel):
 * - NO se cuadra por porcentaje de stock
 * - Se cuadra cuando recaudado >= inversión de Samuel
 * - 20% stock = solo alerta informativa
 * 
 * TANDA 2:
 * - En lotes de 2 tandas: 20% stock = trigger (inversión vendedor + ganancias)
 * - En lotes de 3 tandas: 10% stock = trigger (inversión vendedor)
 * 
 * TANDA 3:
 * - 20% stock = trigger (ganancias puras)
 * - stock = 0 → mini-cuadre final
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

    /**
     * Genera un cuadre para una tanda.
     * 
     * @param tandaId ID de la tanda
     * @param forzar Si es true, ignora validaciones de trigger
     */
    @Transactional
    public CuadreResponse generarCuadre(Long tandaId, boolean forzar) {
        Tanda tanda = tandaRepository.findByIdWithLote(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));

        if (!"LIBERADA".equals(tanda.getEstado())) {
            throw new ValidacionNegocioException("Solo se puede generar cuadre de tandas liberadas");
        }

        if (cuadreRepository.existsByTandaIdAndEstado(tandaId, "PENDIENTE")) {
            throw new ValidacionNegocioException("Ya existe un cuadre pendiente para esta tanda");
        }

        // Validar si puede cuadrar
        if (!forzar) {
            validarPuedeGenerarCuadre(tanda);
        }

        Lote lote = tanda.getLote();
        TipoCuadre tipoCuadre = determinarTipoCuadre(tanda);

        CalculoCuadreResponse calculo = calculadorService.calcular(tanda, tipoCuadre);
        String textoWhatsApp = whatsAppService.generarTexto(tanda, calculo);

        Cuadre cuadre = Cuadre.builder()
                .tanda(tanda)
                .tipo(tipoCuadre)
                .totalRecaudado(calculo.getTotalRecaudado())
                .montoEsperado(calculo.getMontoQueDebeTransferir())
                .montoVendedor(calculo.getMontoParaVendedor())
                .montoCascada(tipoCuadre == TipoCuadre.GANANCIA && lote.esModelo50_50()
                        ? calculo.getDisponibleTotal().subtract(calculo.getMontoParaVendedor()) : null)
                .excedenteAnterior(calculo.getExcedenteAnterior())
                .excedente(calculo.getExcedenteResultante())
                .textoWhatsapp(textoWhatsApp)
                .estado("PENDIENTE")
                .build();

        cuadre = cuadreRepository.save(cuadre);

        tanda.setEstado("EN_CUADRE");
        tandaRepository.save(tanda);

        log.info("🔔 Cuadre generado: ID={}, Tanda={}/{}, Tipo={}, MontoEsperado={}",
                cuadre.getId(), tanda.getNumero(), lote.getNumeroTandas(), 
                tipoCuadre, cuadre.getMontoEsperado());

        return mapToResponse(cuadre);
    }

    /**
     * Valida si se puede generar cuadre para una tanda.
     */
    private void validarPuedeGenerarCuadre(Tanda tanda) {
        int numeroTanda = tanda.getNumero();
        
        if (numeroTanda == 1) {
            // Tanda 1: Verificar que recaudado >= inversión Samuel
            if (!calculadorService.puedeHacerCuadreTanda1(tanda)) {
                BigDecimal recaudado = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());
                BigDecimal inversionSamuel = tanda.getLote().getInversionSamuel();
                throw new ValidacionNegocioException(
                        String.format("Recaudado ($%,.0f) insuficiente para inversión Samuel ($%,.0f)",
                                recaudado != null ? recaudado : BigDecimal.ZERO, inversionSamuel));
            }
        } else {
            // Tandas 2+: Verificar por porcentaje de stock
            if (!tanda.requiereCuadrePorStock()) {
                throw new ValidacionNegocioException(
                        String.format("Stock debe estar en %d%% o menos. Actual: %.1f%%",
                                tanda.getPorcentajeTrigger(), tanda.getPorcentajeStockRestante()));
            }
        }
    }

    /**
     * Determina el tipo de cuadre según la tanda.
     */
    private TipoCuadre determinarTipoCuadre(Tanda tanda) {
        int numero = tanda.getNumero();
        int totalTandas = tanda.getTotalTandas();

        if (numero == 1) {
            return TipoCuadre.INVERSION;
        } else if (totalTandas == 2 && numero == 2) {
            // T2 de 2 tandas: inversión vendedor + ganancias = mixto, usamos GANANCIA
            return TipoCuadre.GANANCIA;
        } else if (totalTandas == 3 && numero == 2) {
            // T2 de 3 tandas: inversión vendedor = INVERSION
            return TipoCuadre.INVERSION;
        } else {
            // T3: ganancias puras
            return TipoCuadre.GANANCIA;
        }
    }

    /**
     * Confirma un cuadre (admin recibió el dinero).
     */
    @Transactional
    public CuadreResponse confirmarCuadre(Long cuadreId, ConfirmarCuadreRequest request) {
        Cuadre cuadre = cuadreRepository.findById(cuadreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", cuadreId));

        if (!cuadre.estaPendiente() && !"EN_PROCESO".equals(cuadre.getEstado())) {
            throw new ValidacionNegocioException("El cuadre no está pendiente de confirmación");
        }

        // Si montoEsperado es 0 (como en T2 de inversión vendedor), no validar monto
        if (cuadre.getMontoEsperado().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getMontoRecibido().compareTo(cuadre.getMontoEsperado()) < 0) {
                throw new ValidacionNegocioException(
                        String.format("El monto recibido ($%,.0f) es menor al esperado ($%,.0f)",
                                request.getMontoRecibido(), cuadre.getMontoEsperado()));
            }
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

        // Liberar siguiente tanda si aplica
        liberarSiguienteTandaSiAplica(tanda.getLote());
        
        // Verificar si el lote está completado
        verificarLoteCompletado(tanda.getLote());

        log.info("✅ Cuadre confirmado: ID={}, MontoRecibido={}", cuadreId, request.getMontoRecibido());

        return mapToResponse(cuadre);
    }

    /**
     * Libera la siguiente tanda si la anterior está cuadrada.
     */
    private void liberarSiguienteTandaSiAplica(Lote lote) {
        List<Tanda> tandas = tandaRepository.findByLoteIdOrderByNumeroAsc(lote.getId());

        for (Tanda tanda : tandas) {
            if ("PENDIENTE".equals(tanda.getEstado())) {
                boolean puedeLiberar = false;

                if (tanda.getNumero() == 1) {
                    puedeLiberar = true; // Primera tanda siempre puede liberarse
                } else {
                    // Verificar que la anterior esté cuadrada
                    puedeLiberar = tandaRepository.isTandaCuadrada(lote.getId(), tanda.getNumero() - 1);
                }

                if (puedeLiberar) {
                    tanda.setEstado("LIBERADA");
                    tanda.setStockEntregado(tanda.getCantidadAsignada());
                    tanda.setStockActual(tanda.getCantidadAsignada());
                    tanda.setFechaLiberacion(LocalDateTime.now());
                    tandaRepository.save(tanda);
                    log.info("📦 Tanda liberada: Lote={}, Tanda={}/{}", 
                            lote.getId(), tanda.getNumero(), lote.getNumeroTandas());
                }
                break; // Solo liberar una a la vez
            }
        }
    }

    /**
     * Verifica si todas las tandas del lote están cuadradas.
     */
    private void verificarLoteCompletado(Lote lote) {
        List<Tanda> tandas = tandaRepository.findByLoteIdOrderByNumeroAsc(lote.getId());
        boolean todasCuadradas = tandas.stream().allMatch(t -> "CUADRADA".equals(t.getEstado()));

        if (todasCuadradas) {
            lote.setEstado("COMPLETADO");
            loteRepository.save(lote);
            log.info("🎉 Lote completado: ID={}", lote.getId());
        }
    }

    /**
     * Obtiene un cuadre por ID.
     */
    @Transactional(readOnly = true)
    public CuadreResponse obtenerCuadre(Long id) {
        Cuadre cuadre = cuadreRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", id));
        return mapToResponse(cuadre);
    }

    /**
     * Lista cuadres pendientes.
     */
    @Transactional(readOnly = true)
    public Page<CuadreResponse> listarCuadresPendientes(Pageable pageable) {
        return cuadreRepository.findByEstado("PENDIENTE", pageable).map(this::mapToResponse);
    }

    /**
     * Lista cuadres de un usuario.
     */
    @Transactional(readOnly = true)
    public List<CuadreResponse> listarCuadresDeUsuario(Long usuarioId) {
        return cuadreRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Lista cuadres de un lote.
     */
    @Transactional(readOnly = true)
    public List<CuadreResponse> listarCuadresDeLote(Long loteId) {
        return cuadreRepository.findByLoteId(loteId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Obtiene texto de WhatsApp de un cuadre.
     */
    @Transactional(readOnly = true)
    public String obtenerTextoWhatsApp(Long cuadreId) {
        Cuadre cuadre = cuadreRepository.findById(cuadreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuadre", cuadreId));
        return cuadre.getTextoWhatsapp();
    }

    /**
     * Detecta tandas que requieren cuadre.
     * 
     * - Tanda 1: Cuando recaudado >= inversión Samuel
     * - Tandas 2+: Cuando stock <= porcentaje de trigger
     */
    @Transactional(readOnly = true)
    public List<CuadreResponse> detectarTandasParaCuadre() {
        List<CuadreResponse> resultado = new ArrayList<>();

        // 1. Buscar Tandas 1 que pueden cuadrarse (por monto)
        List<Tanda> tandas1 = tandaRepository.findTandas1Liberadas();
        for (Tanda t : tandas1) {
            if (!cuadreRepository.existsByTandaIdAndEstado(t.getId(), "PENDIENTE")) {
                if (calculadorService.puedeHacerCuadreTanda1(t)) {
                    resultado.add(buildDeteccionResponse(t, "Recaudado suficiente para inversión Samuel"));
                }
            }
        }

        // 2. Buscar Tandas 2+ que requieren cuadre por stock
        List<Tanda> tandasStock = tandaRepository.findTandasParaCuadrePorStock();
        for (Tanda t : tandasStock) {
            if (!cuadreRepository.existsByTandaIdAndEstado(t.getId(), "PENDIENTE")) {
                resultado.add(buildDeteccionResponse(t, 
                        String.format("Stock en %.1f%% (trigger: %d%%)", 
                                t.getPorcentajeStockRestante(), t.getPorcentajeTrigger())));
            }
        }

        return resultado;
    }

    /**
     * Detecta Tandas 1 en alerta (stock <= 20% pero aún sin suficiente recaudado).
     */
    @Transactional(readOnly = true)
    public List<CuadreResponse> detectarAlertas() {
        List<CuadreResponse> alertas = new ArrayList<>();

        List<Tanda> tandas1Alerta = tandaRepository.findTandas1EnAlerta();
        for (Tanda t : tandas1Alerta) {
            if (!calculadorService.puedeHacerCuadreTanda1(t)) {
                BigDecimal recaudado = ventaRepository.sumarRecaudadoPorTanda(t.getId());
                BigDecimal inversionSamuel = t.getLote().getInversionSamuel();
                
                alertas.add(CuadreResponse.builder()
                        .tanda(CuadreResponse.TandaInfo.builder()
                                .id(t.getId())
                                .numero(t.getNumero())
                                .descripcion(t.getDescripcion())
                                .stockEntregado(t.getStockEntregado())
                                .stockActual(t.getStockActual())
                                .porcentajeRestante(t.getPorcentajeStockRestante())
                                .build())
                        .tipo(TipoCuadre.INVERSION)
                        .estado("ALERTA")
                        .totalRecaudado(recaudado)
                        .montoEsperado(inversionSamuel)
                        .build());
            }
        }

        return alertas;
    }

    private CuadreResponse buildDeteccionResponse(Tanda t, String mensaje) {
        return CuadreResponse.builder()
                .tanda(CuadreResponse.TandaInfo.builder()
                        .id(t.getId())
                        .numero(t.getNumero())
                        .descripcion(t.getDescripcion())
                        .stockEntregado(t.getStockEntregado())
                        .stockActual(t.getStockActual())
                        .porcentajeRestante(t.getPorcentajeStockRestante())
                        .build())
                .tipo(t.esTandaInversion() ? TipoCuadre.INVERSION : TipoCuadre.GANANCIA)
                .estado("REQUIERE_CUADRE")
                .build();
    }

    /**
     * Obtiene resumen de cuadres.
     */
    @Transactional(readOnly = true)
    public ResumenCuadresResponse obtenerResumen() {
        long pendientes = cuadreRepository.countByEstado("PENDIENTE");
        long enProceso = cuadreRepository.countByEstado("EN_PROCESO");
        long exitosos = cuadreRepository.countByEstado("EXITOSO");

        List<Cuadre> cuadresPendientes = cuadreRepository.findByEstadoOrderByCreatedAtAsc("PENDIENTE");

        List<ResumenCuadresResponse.CuadrePendienteInfo> pendientesInfo = cuadresPendientes.stream()
                .map(c -> {
                    Tanda tanda = c.getTanda();
                    Lote lote = tanda.getLote();
                    Usuario vendedor = lote.getUsuario();
                    Duration tiempo = Duration.between(c.getCreatedAt(), LocalDateTime.now());

                    return ResumenCuadresResponse.CuadrePendienteInfo.builder()
                            .cuadreId(c.getId())
                            .vendedorNombre(vendedor.getNombre())
                            .vendedorTelefono(vendedor.getTelefono())
                            .tandaNumero(tanda.getNumero())
                            .totalTandas(lote.getNumeroTandas())
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

    /**
     * Obtiene detalle de cálculo para una tanda.
     */
    @Transactional(readOnly = true)
    public CalculoCuadreResponse obtenerDetalleCalculo(Long tandaId) {
        Tanda tanda = tandaRepository.findByIdWithLote(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));
        TipoCuadre tipo = determinarTipoCuadre(tanda);
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
                        .id(tanda.getId())
                        .numero(tanda.getNumero())
                        .descripcion(tanda.getDescripcion())
                        .stockEntregado(tanda.getStockEntregado())
                        .stockActual(tanda.getStockActual())
                        .porcentajeRestante(tanda.getPorcentajeStockRestante())
                        .build())
                .lote(CuadreResponse.LoteInfo.builder()
                        .id(lote.getId())
                        .cantidadTotal(lote.getCantidadTotal())
                        .modelo(lote.getModelo())
                        .inversionTotal(lote.getInversionPercibidaTotal())
                        .numeroTandas(lote.getNumeroTandas())
                        .build())
                .vendedor(CuadreResponse.VendedorInfo.builder()
                        .id(vendedor.getId())
                        .nombre(vendedor.getNombre())
                        .cedula(vendedor.getCedula())
                        .nivel(vendedor.getNivel())
                        .telefono(vendedor.getTelefono())
                        .build())
                .tipo(cuadre.getTipo())
                .estado(cuadre.getEstado())
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
}
