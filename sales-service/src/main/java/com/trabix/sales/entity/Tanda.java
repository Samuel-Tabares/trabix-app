package com.trabix.sales.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Tanda simplificada para relaciones en sales-service.
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

    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "stock_entregado", nullable = false)
    private Integer stockEntregado;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private String estado;

    /**
     * Reduce el stock de la tanda.
     */
    public void reducirStock(int cantidad) {
        if (cantidad > stockActual) {
            throw new IllegalArgumentException("Stock insuficiente");
        }
        this.stockActual -= cantidad;
    }

    /**
     * Restaura stock (cuando se rechaza una venta).
     */
    public void restaurarStock(int cantidad) {
        this.stockActual += cantidad;
    }
}
