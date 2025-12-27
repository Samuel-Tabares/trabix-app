package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración de costos del sistema.
 * Solo hay un registro activo (singleton en BD).
 * 
 * Modelo de costos TRABIX:
 * - costoRealTrabix: Costo real de producción (suma de componentes: pitillos, fondo, envío, operativos)
 * - costoPercibidoTrabix: Lo que ven los vendedores (lo define el ADMIN)
 * - aporteFondoPorTrabix: $200 por TRABIX que va al fondo cuando VENDEDORES pagan lotes
 * 
 * IMPORTANTE: El costo real y percibido son variables y los digita el ADMIN manualmente.
 */
@Entity
@Table(name = "configuracion_costos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCostos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Costo real de producción por TRABIX.
     * Incluye: pitillos, aporte fondo, envío, gastos operativos.
     * Solo visible para ADMIN.
     */
    @Column(name = "costo_real_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoRealTrabix;

    /**
     * Costo que ven los vendedores por TRABIX.
     * Lo define el ADMIN manualmente.
     */
    @Column(name = "costo_percibido_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoTrabix;

    /**
     * Aporte al fondo de recompensas por cada TRABIX.
     * Se cobra cuando un VENDEDOR paga un lote (no cuando el ADMIN vende).
     * Default: $200
     */
    @Column(name = "aporte_fondo_por_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal aporteFondoPorTrabix;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
        fechaActualizacion = now;
    }

    @PreUpdate
    protected void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        updatedAt = now;
        fechaActualizacion = now;
    }

    /**
     * Diferencia entre costo percibido y real.
     * Este es el margen bruto por unidad.
     */
    public BigDecimal getDiferenciaCosto() {
        if (costoPercibidoTrabix == null || costoRealTrabix == null) {
            return BigDecimal.ZERO;
        }
        return costoPercibidoTrabix.subtract(costoRealTrabix);
    }

    /**
     * Calcula el costo total de un lote según costo percibido.
     */
    public BigDecimal calcularCostoLote(int cantidadTrabix) {
        if (cantidadTrabix <= 0 || costoPercibidoTrabix == null) {
            return BigDecimal.ZERO;
        }
        return costoPercibidoTrabix.multiply(BigDecimal.valueOf(cantidadTrabix));
    }

    /**
     * Calcula aporte al fondo por lote.
     */
    public BigDecimal calcularAporteFondoPorLote(int cantidadTrabix) {
        if (cantidadTrabix <= 0 || aporteFondoPorTrabix == null) {
            return BigDecimal.ZERO;
        }
        return aporteFondoPorTrabix.multiply(BigDecimal.valueOf(cantidadTrabix));
    }

    /**
     * Valida que la configuración sea coherente.
     */
    public boolean esValida() {
        if (costoRealTrabix == null || costoPercibidoTrabix == null || aporteFondoPorTrabix == null) {
            return false;
        }
        if (costoRealTrabix.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (costoPercibidoTrabix.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (aporteFondoPorTrabix.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return true;
    }
}
