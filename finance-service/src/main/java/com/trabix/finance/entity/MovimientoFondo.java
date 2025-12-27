package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento del Fondo de Recompensas.
 * 
 * Registra cada ingreso y egreso del fondo.
 * Los movimientos son inmutables una vez creados.
 * 
 * INGRESOS: Cuando un VENDEDOR paga un lote ($200 × TRABIX)
 * EGRESOS: Premios, incentivos, bonificaciones, retiros
 */
@Entity
@Table(name = "movimientos_fondo", indexes = {
    @Index(name = "idx_movfondo_fecha", columnList = "fecha"),
    @Index(name = "idx_movfondo_tipo", columnList = "tipo"),
    @Index(name = "idx_movfondo_beneficiario", columnList = "beneficiario_id"),
    @Index(name = "idx_movfondo_referencia", columnList = "referencia_tipo, referencia_id"),
    @Index(name = "idx_movfondo_fondo_fecha", columnList = "fondo_id, fecha DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoFondo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fondo_id", nullable = false)
    @ToString.Exclude
    private FondoRecompensas fondo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoFondo tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Beneficiario del movimiento.
     * Solo aplica para egresos tipo PREMIO, INCENTIVO, BONIFICACION.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id")
    @ToString.Exclude
    private Usuario beneficiario;

    /**
     * Vendedor que originó el ingreso (para pagos de lote).
     * Solo aplica para ingresos tipo PAGO_LOTE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_origen_id")
    @ToString.Exclude
    private Usuario vendedorOrigen;

    /**
     * Saldo del fondo después de este movimiento.
     */
    @Column(name = "saldo_posterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPosterior;

    /**
     * ID de referencia externa (ej: ID del lote, cuadre, etc.).
     */
    @Column(name = "referencia_id")
    private Long referenciaId;

    /**
     * Tipo de referencia externa.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_tipo", length = 20)
    private ReferenciaMovimiento referenciaTipo;

    /**
     * Cantidad de TRABIX asociados (para pagos de lote).
     */
    @Column(name = "cantidad_trabix")
    private Integer cantidadTrabix;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    /**
     * Verifica si es un movimiento de ingreso.
     */
    public boolean esIngreso() {
        return TipoMovimientoFondo.INGRESO.equals(tipo);
    }

    /**
     * Verifica si es un movimiento de egreso.
     */
    public boolean esEgreso() {
        return TipoMovimientoFondo.EGRESO.equals(tipo);
    }

    /**
     * Verifica si tiene beneficiario asignado.
     */
    public boolean tieneBeneficiario() {
        return beneficiario != null;
    }

    /**
     * Verifica si tiene vendedor origen (para pagos de lote).
     */
    public boolean tieneVendedorOrigen() {
        return vendedorOrigen != null;
    }

    /**
     * Verifica si es un premio (egreso con beneficiario).
     */
    public boolean esPremio() {
        return esEgreso() && tieneBeneficiario() && 
               ReferenciaMovimiento.PREMIO.equals(referenciaTipo);
    }

    /**
     * Verifica si es un ingreso por pago de lote de vendedor.
     */
    public boolean esPagoLoteVendedor() {
        return esIngreso() && ReferenciaMovimiento.PAGO_LOTE.equals(referenciaTipo);
    }

    /**
     * Verifica si tiene referencia externa.
     */
    public boolean tieneReferencia() {
        return referenciaId != null && referenciaTipo != null;
    }

    /**
     * Obtiene descripción formateada del movimiento.
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.name()).append(": $").append(monto);
        sb.append(" - ").append(descripcion);
        
        if (tieneBeneficiario()) {
            sb.append(" [Beneficiario: ").append(beneficiario.getNombre()).append("]");
        }
        
        if (tieneVendedorOrigen()) {
            sb.append(" [Vendedor: ").append(vendedorOrigen.getNombre()).append("]");
        }
        
        if (cantidadTrabix != null && cantidadTrabix > 0) {
            sb.append(" [").append(cantidadTrabix).append(" TRABIX]");
        }
        
        return sb.toString();
    }
}
