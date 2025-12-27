package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fondo de Recompensas TRABIX.
 * 
 * El fondo se alimenta SOLO cuando un VENDEDOR paga un lote.
 * El dinero del ADMIN/dueño NUNCA va al fondo.
 * 
 * Por cada TRABIX del lote se agregan $200 (configurable).
 * Se usa para premios e incentivos a vendedores.
 * 
 * IMPORTANTE: Solo debe existir UN registro de fondo en la BD.
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

    /**
     * Saldo actual del fondo.
     * Siempre debe ser >= 0.
     */
    @Column(name = "saldo_actual", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    /**
     * Total acumulado de ingresos históricos.
     */
    @Column(name = "total_ingresos_historico", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalIngresosHistorico = BigDecimal.ZERO;

    /**
     * Total acumulado de egresos históricos.
     */
    @Column(name = "total_egresos_historico", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalEgresosHistorico = BigDecimal.ZERO;

    /**
     * Contador de movimientos totales.
     */
    @Column(name = "total_movimientos", nullable = false)
    @Builder.Default
    private Long totalMovimientos = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Control de concurrencia optimista.
     * Previene actualizaciones perdidas.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (saldoActual == null) {
            saldoActual = BigDecimal.ZERO;
        }
        if (totalIngresosHistorico == null) {
            totalIngresosHistorico = BigDecimal.ZERO;
        }
        if (totalEgresosHistorico == null) {
            totalEgresosHistorico = BigDecimal.ZERO;
        }
        if (totalMovimientos == null) {
            totalMovimientos = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Ingresa dinero al fondo.
     * @param monto Monto a ingresar (debe ser positivo)
     * @return Nuevo saldo después del ingreso
     * @throws IllegalArgumentException si el monto no es válido
     */
    public BigDecimal ingresar(BigDecimal monto) {
        validarMontoPositivo(monto);
        
        this.saldoActual = this.saldoActual.add(monto);
        this.totalIngresosHistorico = this.totalIngresosHistorico.add(monto);
        this.totalMovimientos++;
        
        return this.saldoActual;
    }

    /**
     * Retira dinero del fondo.
     * @param monto Monto a retirar (debe ser positivo y <= saldo)
     * @return Nuevo saldo después del retiro
     * @throws IllegalArgumentException si el monto no es válido o excede el saldo
     */
    public BigDecimal retirar(BigDecimal monto) {
        validarMontoPositivo(monto);
        
        if (!tieneSaldoSuficiente(monto)) {
            throw new IllegalArgumentException(
                String.format("Saldo insuficiente. Disponible: $%s, Solicitado: $%s", 
                    this.saldoActual, monto)
            );
        }
        
        this.saldoActual = this.saldoActual.subtract(monto);
        this.totalEgresosHistorico = this.totalEgresosHistorico.add(monto);
        this.totalMovimientos++;
        
        return this.saldoActual;
    }

    /**
     * Verifica si hay saldo suficiente para un retiro.
     */
    public boolean tieneSaldoSuficiente(BigDecimal monto) {
        if (monto == null) {
            return false;
        }
        return this.saldoActual.compareTo(monto) >= 0;
    }

    /**
     * Obtiene el balance histórico (ingresos - egresos).
     * Debería coincidir con el saldo actual.
     */
    public BigDecimal getBalanceHistorico() {
        return totalIngresosHistorico.subtract(totalEgresosHistorico);
    }

    /**
     * Verifica consistencia del fondo.
     * El saldo actual debe coincidir con el balance histórico.
     */
    public boolean esConsistente() {
        return saldoActual.compareTo(getBalanceHistorico()) == 0;
    }

    /**
     * Verifica si el fondo tiene saldo positivo.
     */
    public boolean tieneSaldo() {
        return saldoActual.compareTo(BigDecimal.ZERO) > 0;
    }

    private void validarMontoPositivo(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto no puede ser null");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
    }
}
