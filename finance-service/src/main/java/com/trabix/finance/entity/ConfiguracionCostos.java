package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración de costos del sistema.
 * Solo hay un registro activo, controlado exclusivamente por el admin.
 * 
 * Campos clave:
 * - costo_real_trabix: Costo real de producción (variable, default $2,000)
 * - costo_percibido_trabix: Lo que ven los vendedores ($2,400 fijo)
 * - aporte_fondo_por_trabix: $200 por TRABIX vendido va al fondo
 * - aporte_gestion_por_trabix: $200 para gestión operativa
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

    /** Costo real de producción por TRABIX (variable, ej: $2,000) */
    @Column(name = "costo_real_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoRealTrabix;

    /** Costo que ven los vendedores por TRABIX ($2,400 fijo) */
    @Column(name = "costo_percibido_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoTrabix;

    /** Aporte al fondo de recompensas por cada TRABIX vendido ($200) */
    @Column(name = "aporte_fondo_por_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal aporteFondoPorTrabix;

    /** Aporte para gestión operativa por cada TRABIX ($200) */
    @Column(name = "aporte_gestion_por_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal aporteGestionPorTrabix;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Calcula la diferencia entre costo percibido y real.
     * Esta diferencia es parte de la ganancia oculta de Samuel.
     */
    public BigDecimal getDiferenciaCosto() {
        return costoPercibidoTrabix.subtract(costoRealTrabix);
    }

    /**
     * Calcula el margen total por TRABIX.
     * Incluye: diferencia de costo + aporte fondo + aporte gestión
     */
    public BigDecimal getMargenTotalPorTrabix() {
        return getDiferenciaCosto()
                .add(aporteFondoPorTrabix)
                .add(aporteGestionPorTrabix);
    }

    /**
     * Calcula la ganancia bruta de Samuel por lote.
     * @param cantidadTrabix cantidad de TRABIX en el lote
     */
    public BigDecimal calcularGananciaBrutaPorLote(int cantidadTrabix) {
        return getMargenTotalPorTrabix().multiply(BigDecimal.valueOf(cantidadTrabix));
    }

    /**
     * Calcula el aporte total al fondo por lote.
     */
    public BigDecimal calcularAporteFondoPorLote(int cantidadTrabix) {
        return aporteFondoPorTrabix.multiply(BigDecimal.valueOf(cantidadTrabix));
    }
}
