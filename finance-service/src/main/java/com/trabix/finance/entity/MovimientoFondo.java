package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento del Fondo de Recompensas.
 * Tipos: INGRESO, EGRESO
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

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id")
    @ToString.Exclude
    private Usuario beneficiario;

    @Column(name = "saldo_posterior", precision = 12, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "referencia_id")
    private Long referenciaId;

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

    public boolean esIngreso() {
        return "INGRESO".equals(tipo);
    }

    public boolean esEgreso() {
        return "EGRESO".equals(tipo);
    }

    public boolean tieneBeneficiario() {
        return beneficiario != null;
    }
}
