package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de Costos de Producción.
 * 
 * Permite llevar un control de todos los gastos operativos:
 * - PRODUCCION: Costos directos de fabricación
 * - INSUMO: Materiales y suministros (pitillos, etc.)
 * - MARKETING: Publicidad y promoción
 * - OPERATIVO: Gastos operativos generales
 * - ENVIO: Costos de envío
 * - OTRO: Gastos varios
 * 
 * El ADMIN registra estos gastos manualmente.
 */
@Entity
@Table(name = "costos_produccion", indexes = {
    @Index(name = "idx_costo_fecha", columnList = "fecha"),
    @Index(name = "idx_costo_tipo", columnList = "tipo"),
    @Index(name = "idx_costo_tipo_fecha", columnList = "tipo, fecha")
})
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCosto tipo;

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

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (fecha == null) {
            fecha = now;
        }
        
        calcularCostoTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calcularCostoTotal();
    }

    /**
     * Calcula el costo total basado en cantidad y costo unitario.
     * Protegido contra valores null.
     */
    public void calcularCostoTotal() {
        if (costoUnitario != null && cantidad != null && cantidad > 0) {
            this.costoTotal = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        } else {
            this.costoTotal = BigDecimal.ZERO;
        }
    }

    /**
     * Recalcula el total después de modificaciones.
     */
    public void recalcularTotal() {
        if (costoUnitario == null) {
            throw new IllegalStateException("El costo unitario no puede ser null");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalStateException("La cantidad debe ser mayor a 0");
        }
        calcularCostoTotal();
    }

    /**
     * Actualiza cantidad y recalcula total.
     */
    public void actualizarCantidad(Integer nuevaCantidad) {
        if (nuevaCantidad == null || nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.cantidad = nuevaCantidad;
        calcularCostoTotal();
    }

    /**
     * Actualiza costo unitario y recalcula total.
     */
    public void actualizarCostoUnitario(BigDecimal nuevoCosto) {
        if (nuevoCosto == null || nuevoCosto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El costo unitario debe ser mayor a 0");
        }
        this.costoUnitario = nuevoCosto;
        calcularCostoTotal();
    }

    /**
     * Verifica si el registro tiene factura asociada.
     */
    public boolean tieneFactura() {
        return numeroFactura != null && !numeroFactura.isBlank();
    }

    /**
     * Verifica si el registro tiene proveedor.
     */
    public boolean tieneProveedor() {
        return proveedor != null && !proveedor.isBlank();
    }

    /**
     * Obtiene descripción completa del costo.
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(concepto);
        sb.append(" - ").append(cantidad).append(" x $").append(costoUnitario);
        sb.append(" = $").append(costoTotal);
        sb.append(" (").append(tipo.getDescripcion()).append(")");
        return sb.toString();
    }
}
