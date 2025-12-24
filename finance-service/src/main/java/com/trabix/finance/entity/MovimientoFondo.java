package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento del Fondo de Recompensas.
 * 
 * Registra cada ingreso (por ventas) y egreso (premios) del fondo.
 * Proporciona transparencia total sobre el uso del fondo.
 */
@Entity
@Table(name = "movimientos_fondo")
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

    /** Tipo: INGRESO o EGRESO */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** Monto del movimiento (siempre positivo) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Fecha y hora del movimiento */
    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Descripción/razón del movimiento */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /** Usuario beneficiado (para premios) - puede ser null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id")
    @ToString.Exclude
    private Usuario beneficiario;

    /** Saldo del fondo después del movimiento */
    @Column(name = "saldo_posterior", precision = 12, scale = 2)
    private BigDecimal saldoPosterior;

    /** Referencia opcional (ID de cuadre, lote, etc.) */
    @Column(name = "referencia_id")
    private Long referenciaId;

    /** Tipo de referencia (CUADRE, LOTE, EVENTO, etc.) */
    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

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
     * Verifica si es un ingreso.
     */
    public boolean esIngreso() {
        return "INGRESO".equals(tipo);
    }

    /**
     * Verifica si es un egreso.
     */
    public boolean esEgreso() {
        return "EGRESO".equals(tipo);
    }

    /**
     * Verifica si tiene beneficiario (es un premio).
     */
    public boolean tieneBeneficiario() {
        return beneficiario != null;
    }
}
