package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración de costos del sistema.
 * Solo hay un registro activo.
 * 
 * Campos:
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

    @Column(name = "costo_real_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoRealTrabix;

    @Column(name = "costo_percibido_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoTrabix;

    @Column(name = "aporte_fondo_por_trabix", nullable = false, precision = 10, scale = 2)
    private BigDecimal aporteFondoPorTrabix;

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
     * Diferencia entre costo percibido y real.
     */
    public BigDecimal getDiferenciaCosto() {
        return costoPercibidoTrabix.subtract(costoRealTrabix);
    }

    /**
     * Margen total por TRABIX.
     */
    public BigDecimal getMargenTotalPorTrabix() {
        return getDiferenciaCosto()
                .add(aporteFondoPorTrabix)
                .add(aporteGestionPorTrabix);
    }

    /**
     * Calcula aporte al fondo por lote.
     */
    public BigDecimal calcularAporteFondoPorLote(int cantidadTrabix) {
        return aporteFondoPorTrabix.multiply(BigDecimal.valueOf(cantidadTrabix));
    }
}
