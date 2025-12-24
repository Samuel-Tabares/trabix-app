package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de Costos de Producción.
 * 
 * Tipos: PRODUCCION, INSUMO, MARKETING, OTRO
 */
@Entity
@Table(name = "costos_produccion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostoProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String concepto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoTotal;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(length = 100)
    private String proveedor;

    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (costoTotal == null && costoUnitario != null && cantidad != null) {
            costoTotal = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public void recalcularTotal() {
        this.costoTotal = this.costoUnitario.multiply(BigDecimal.valueOf(this.cantidad));
    }
}
