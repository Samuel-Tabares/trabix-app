package com.trabix.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de movimientos del stock de producción de Samuel.
 * Permite trazabilidad de entradas (producción) y salidas (entregas a vendedores).
 */
@Entity
@Table(name = "movimientos_stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /**
     * Cantidad del movimiento (siempre positivo).
     */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Stock disponible DESPUÉS del movimiento.
     */
    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    /**
     * Costo unitario (solo para PRODUCCION).
     */
    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    /**
     * Referencia al lote (solo para ENTREGA o DEVOLUCION).
     */
    @Column(name = "lote_id")
    private Long loteId;

    /**
     * Usuario relacionado (vendedor para ENTREGA, null para PRODUCCION).
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    /**
     * Descripción o nota del movimiento.
     */
    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fechaMovimiento == null) {
            fechaMovimiento = LocalDateTime.now();
        }
    }

    public enum TipoMovimiento {
        PRODUCCION,      // Samuel produce nuevos TRABIX
        ENTREGA,         // Samuel entrega TRABIX a un vendedor (liberar tanda)
        DEVOLUCION,      // Vendedor devuelve TRABIX (cancelación de lote)
        AJUSTE_POSITIVO, // Ajuste manual para aumentar stock
        AJUSTE_NEGATIVO, // Ajuste manual para reducir stock (pérdidas, etc.)
        VENTA_DIRECTA    // Samuel vende directamente en eventos
    }
}
