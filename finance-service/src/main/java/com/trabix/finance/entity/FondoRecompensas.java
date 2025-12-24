package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fondo de Recompensas TRABIX.
 * 
 * El fondo se alimenta con $200 por cada TRABIX vendido.
 * Se usa para premios, incentivos y eventos especiales.
 * Solo hay un registro de fondo en el sistema.
 */
@Entity
@Table(name = "fondo_recompensas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FondoRecompensas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Saldo disponible en el fondo */
    @Column(name = "saldo_actual", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fondo", fetch = FetchType.LAZY)
    @OrderBy("fecha DESC")
    @ToString.Exclude
    @Builder.Default
    private List<MovimientoFondo> movimientos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (saldoActual == null) {
            saldoActual = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Ingresa dinero al fondo.
     * @return el nuevo saldo
     */
    public BigDecimal ingresar(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        this.saldoActual = this.saldoActual.add(monto);
        return this.saldoActual;
    }

    /**
     * Retira dinero del fondo (para premios).
     * @return el nuevo saldo
     */
    public BigDecimal retirar(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        if (monto.compareTo(this.saldoActual) > 0) {
            throw new IllegalArgumentException("Saldo insuficiente en el fondo");
        }
        this.saldoActual = this.saldoActual.subtract(monto);
        return this.saldoActual;
    }

    /**
     * Verifica si hay saldo suficiente para un retiro.
     */
    public boolean tieneSaldoSuficiente(BigDecimal monto) {
        return this.saldoActual.compareTo(monto) >= 0;
    }
}
