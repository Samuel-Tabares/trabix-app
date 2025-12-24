package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de Costos de Producción.
 * 
 * Permite al admin registrar todos los gastos del negocio:
 * - PRODUCCION: granizados, pitillos, vasos, etc.
 * - INSUMO: licor, mezclas, hielo, etc.
 * - MARKETING: publicidad, material promocional
 * - OTRO: gastos varios
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

    /** Descripción del gasto (granizados, pitillos, etc.) */
    @Column(nullable = false, length = 100)
    private String concepto;

    /** Cantidad comprada/utilizada */
    @Column(nullable = false)
    private Integer cantidad;

    /** Precio por unidad */
    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    /** Total = cantidad × costo_unitario */
    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoTotal;

    /** Tipo: PRODUCCION, INSUMO, MARKETING, OTRO */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** Fecha del gasto */
    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Nota adicional opcional */
    @Column(columnDefinition = "TEXT")
    private String nota;

    /** Proveedor (opcional) */
    @Column(length = 100)
    private String proveedor;

    /** Número de factura/recibo (opcional) */
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
        // Calcular costo total si no está definido
        if (costoTotal == null && costoUnitario != null && cantidad != null) {
            costoTotal = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    /**
     * Recalcula el costo total.
     */
    public void recalcularTotal() {
        this.costoTotal = this.costoUnitario.multiply(BigDecimal.valueOf(this.cantidad));
    }

    /**
     * Verifica si es un costo de producción.
     */
    public boolean esProduccion() {
        return "PRODUCCION".equals(tipo);
    }

    /**
     * Verifica si es un insumo.
     */
    public boolean esInsumo() {
        return "INSUMO".equals(tipo);
    }

    /**
     * Verifica si es marketing.
     */
    public boolean esMarketing() {
        return "MARKETING".equals(tipo);
    }
}
