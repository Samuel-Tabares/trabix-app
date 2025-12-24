package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Tanda para billing-service.
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

    /**
     * Calcula el porcentaje de stock restante.
     */
    public double getPorcentajeStockRestante() {
        if (stockEntregado == 0) return 100.0;
        return (stockActual * 100.0) / stockEntregado;
    }

    /**
     * Verifica si la tanda requiere cuadre (stock <= 20%).
     */
    public boolean requiereCuadre(int porcentajeTrigger) {
        return "LIBERADA".equals(estado) && getPorcentajeStockRestante() <= porcentajeTrigger;
    }

    /**
     * Verifica si es la primera tanda (cuadre de inversión).
     */
    public boolean esTandaInversion() {
        return numero == 1;
    }

    /**
     * Verifica si es tanda de ganancias (2 o 3).
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
}
