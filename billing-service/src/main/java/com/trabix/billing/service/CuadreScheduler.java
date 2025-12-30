package com.trabix.billing.service;

import com.trabix.billing.entity.Tanda;
import com.trabix.billing.repository.CuadreRepository;
import com.trabix.billing.repository.TandaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio programado para detectar tandas que requieren cuadre.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuadreScheduler {

    private final TandaRepository tandaRepository;
    private final CuadreRepository cuadreRepository;
    private final CuadreService cuadreService;
    private final CalculadorCuadreService calculadorService;

    @Value("${trabix.cuadre-automatico:false}")
    private boolean cuadreAutomatico;

    /**
     * Verifica cada 5 minutos si hay tandas que requieren cuadre.
     * 
     * - Tanda 1: Por monto recaudado (no por stock)
     * - Tandas 2+: Por porcentaje de stock
     */
    @Scheduled(fixedRate = 300000)
    @Transactional(readOnly = true)
    public void verificarTandasParaCuadre() {
        // 1. Verificar Tandas 2+ por stock
        List<Tanda> tandasPorStock = tandaRepository.findTandasParaCuadrePorStock();

        for (Tanda tanda : tandasPorStock) {
            if (cuadreRepository.existsByTandaIdAndEstado(tanda.getId(), "PENDIENTE")) {
                continue;
            }

            log.info("🔔 Tanda {}/{} requiere cuadre por stock: Lote={}, Stock={}%, Vendedor={}",
                    tanda.getNumero(), tanda.getTotalTandas(),
                    tanda.getLote().getId(),
                    String.format("%.1f", tanda.getPorcentajeStockRestante()),
                    tanda.getLote().getUsuario().getNombre());

            if (cuadreAutomatico) {
                generarCuadreAutomatico(tanda);
            }
        }

        // 2. Verificar Tandas 1 por monto
        List<Tanda> tandas1 = tandaRepository.findTandas1Liberadas();
        
        for (Tanda tanda : tandas1) {
            if (cuadreRepository.existsByTandaIdAndEstado(tanda.getId(), "PENDIENTE")) {
                continue;
            }

            if (calculadorService.puedeHacerCuadreTanda1(tanda)) {
                log.info("🔔 Tanda 1 requiere cuadre por monto: Lote={}, Vendedor={}",
                        tanda.getLote().getId(),
                        tanda.getLote().getUsuario().getNombre());

                if (cuadreAutomatico) {
                    generarCuadreAutomatico(tanda);
                }
            }
        }
    }

    /**
     * Genera cuadre automático para una tanda.
     */
    private void generarCuadreAutomatico(Tanda tanda) {
        try {
            cuadreService.generarCuadre(tanda.getId(), false);
            log.info("✅ Cuadre generado automáticamente para tanda {}", tanda.getId());
        } catch (Exception e) {
            log.error("Error generando cuadre automático: {}", e.getMessage());
        }
    }

    /**
     * Verificación diaria de cuadres pendientes.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void verificacionDiaria() {
        long pendientes = cuadreRepository.countByEstado("PENDIENTE");
        if (pendientes > 0) {
            log.warn("⚠️ Hay {} cuadres pendientes. Revisar manualmente.", pendientes);
        }

        // Verificar alertas de T1 con stock bajo
        List<Tanda> alertas = tandaRepository.findTandas1EnAlerta();
        for (Tanda t : alertas) {
            if (!calculadorService.puedeHacerCuadreTanda1(t)) {
                log.warn("⚠️ ALERTA: Tanda 1 del Lote {} tiene {}% de stock pero no puede cuadrarse (recaudado insuficiente)",
                        t.getLote().getId(), 
                        String.format("%.1f", t.getPorcentajeStockRestante()));
            }
        }
    }
}
