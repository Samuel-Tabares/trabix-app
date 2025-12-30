package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Tanda para billing-service.
 * 
 * ═══════════════════════════════════════════════════════════════════
 * TRIGGERS DE CUADRE:
 * 
 * TANDA 1:
 * - Trigger: Recaudado >= Inversión de Samuel
 * - NO se cuadra por porcentaje de stock
 * - 20% stock = solo ALERTA informativa
 * 
 * 2 TANDAS (≤50 TRABIX):
 * - T2: 20% stock = trigger cuadre (inversión vendedor + ganancias)
 * 
 * 3 TANDAS (>50 TRABIX):
 * - T2: 10% stock = trigger cuadre (inversión vendedor + ganancias)
 * - T3: 20% stock = trigger cuadre (ganancias puras)
 * ═══════════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "tandas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    @ToString.Exclude
    private Lote lote;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "cantidad_asignada", nullable = false)
    private Integer cantidadAsignada;

    @Column(name = "stock_entregado", nullable = false)
    private Integer stockEntregado;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    @Column(nullable = false, length = 20)
    private String estado;

    // === Umbrales de trigger ===
    private static final int TANDA1_ALERTA_PORCENTAJE = 20;
    private static final int TANDA2_INTERMEDIA_CUADRE = 10;
    private static final int TANDA_FINAL_CUADRE = 20;

    /**
     * Calcula el porcentaje de stock restante.
     */
    public double getPorcentajeStockRestante() {
        if (stockEntregado == 0) return 100.0;
        return (stockActual * 100.0) / stockEntregado;
    }

    /**
     * Obtiene el número total de tandas del lote.
     */
    public int getTotalTandas() {
        return lote != null ? lote.getNumeroTandas() : 3;
    }

    /**
     * Verifica si es la última tanda del lote.
     */
    public boolean esUltimaTanda() {
        return numero == getTotalTandas();
    }

    /**
     * Verifica si es tanda intermedia (solo aplica con 3 tandas).
     */
    public boolean esTandaIntermedia() {
        return getTotalTandas() == 3 && numero == 2;
    }

    /**
     * Verifica si la tanda requiere cuadre por porcentaje de stock.
     * 
     * IMPORTANTE: Tanda 1 NO usa este método - su cuadre es por monto recaudado.
     * Este método solo aplica para Tandas 2+.
     */
    public boolean requiereCuadrePorStock() {
        if (!"LIBERADA".equals(estado)) return false;
        
        double porcentaje = getPorcentajeStockRestante();
        int totalTandas = getTotalTandas();

        // Tanda 1: NO se cuadra por porcentaje, solo por monto recaudado
        if (numero == 1) {
            return false;
        }

        if (totalTandas == 2) {
            // 2 tandas: T2 es la final, trigger al 20%
            return porcentaje <= TANDA_FINAL_CUADRE;
        } else {
            // 3 tandas
            if (numero == 2) {
                // T2 intermedia: trigger al 10%
                return porcentaje <= TANDA2_INTERMEDIA_CUADRE;
            } else {
                // T3 final: trigger al 20%
                return porcentaje <= TANDA_FINAL_CUADRE;
            }
        }
    }

    /**
     * Verifica si T1 está en nivel de alerta (20% stock).
     * Solo informativo - cuadre real es por monto, no por stock.
     */
    public boolean tanda1EnAlerta() {
        return numero == 1 && "LIBERADA".equals(estado) 
            && getPorcentajeStockRestante() <= TANDA1_ALERTA_PORCENTAJE;
    }

    /**
     * Verifica si es la primera tanda (cuadre de inversión Samuel).
     */
    public boolean esTandaInversion() {
        return numero == 1;
    }

    /**
     * Verifica si es tanda de ganancias (2+).
     */
    public boolean esTandaGanancias() {
        return numero > 1;
    }

    /**
     * Calcula unidades vendidas en esta tanda.
     */
    public int getUnidadesVendidas() {
        return stockEntregado - stockActual;
    }

    /**
     * Obtiene descripción de la tanda según su número y total de tandas.
     */
    public String getDescripcion() {
        int total = getTotalTandas();
        
        if (total == 2) {
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Inversión vendedor + Ganancias)";
                default -> "Tanda " + numero;
            };
        } else {
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Inversión vendedor + Ganancias)";
                case 3 -> "Tanda 3 (Ganancias puras)";
                default -> "Tanda " + numero;
            };
        }
    }

    /**
     * Obtiene el porcentaje de trigger para esta tanda.
     */
    public int getPorcentajeTrigger() {
        if (numero == 1) return TANDA1_ALERTA_PORCENTAJE; // Solo alerta, no trigger real
        if (esTandaIntermedia()) return TANDA2_INTERMEDIA_CUADRE;
        return TANDA_FINAL_CUADRE;
    }
}
