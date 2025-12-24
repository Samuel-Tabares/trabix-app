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
 * Ejecuta periódicamente y genera alertas/cuadres automáticos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuadreScheduler {

    private final TandaRepository tandaRepository;
    private final CuadreRepository cuadreRepository;
    private final CuadreService cuadreService;

    @Value("${trabix.trigger-cuadre-porcentaje:20}")
    private int triggerCuadrePorcentaje;

    @Value("${trabix.cuadre-automatico:false}")
    private boolean cuadreAutomatico;

    /**
     * Verifica cada 5 minutos si hay tandas que requieren cuadre.
     * En producción, solo alerta. El cuadre lo genera el admin.
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional(readOnly = true)
    public void verificarTandasParaCuadre() {
        List<Tanda> tandas = tandaRepository.findTandasParaCuadre(triggerCuadrePorcentaje);

        for (Tanda tanda : tandas) {
            // Verificar si ya tiene cuadre pendiente
            if (cuadreRepository.existsByTandaIdAndEstado(tanda.getId(), "PENDIENTE")) {
                continue;
            }

            log.info("🔔 Tanda requiere cuadre: Lote={}, Tanda={}, Stock={}%, Vendedor={}",
                    tanda.getLote().getId(),
                    tanda.getNumero(),
                    String.format("%.1f", tanda.getPorcentajeStockRestante()),
                    tanda.getLote().getUsuario().getNombre());

            // Si está habilitado el cuadre automático, generarlo
            if (cuadreAutomatico) {
                try {
                    cuadreService.generarCuadre(tanda.getId(), false);
                    log.info("✅ Cuadre generado automáticamente para tanda {}", tanda.getId());
                } catch (Exception e) {
                    log.error("Error generando cuadre automático: {}", e.getMessage());
                }
            }

            // TODO: Enviar notificación push/email al admin
            // notificationService.notificarCuadreRequerido(tanda);
        }
    }

    /**
     * Limpieza diaria de cuadres expirados (más de 7 días pendientes).
     * Solo para casos extremos donde no se completó el cuadre.
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3:00 AM cada día
    @Transactional
    public void limpiarCuadresExpirados() {
        // Por ahora solo log, no cancelamos automáticamente
        long pendientes = cuadreRepository.countByEstado("PENDIENTE");
        if (pendientes > 0) {
            log.warn("⚠️ Hay {} cuadres pendientes. Revisar manualmente.", pendientes);
        }
    }
}
