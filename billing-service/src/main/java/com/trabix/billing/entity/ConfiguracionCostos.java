package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración de costos del sistema.
 * Solo hay un registro, controlado por el admin.
 */
@Entity
@Table(name = "configuracion_costos")
@Data
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

    /**
     * Calcula la diferencia entre costo percibido y real.
     * Esta diferencia es parte de la ganancia oculta de Samuel.
     */
    public BigDecimal getDiferenciaCosto() {
        return costoPercibidoTrabix.subtract(costoRealTrabix);
    }

    /**
     * Calcula el margen total por TRABIX (diferencia + aportes).
     */
    public BigDecimal getMargenTotalPorTrabix() {
        return getDiferenciaCosto()
                .add(aporteFondoPorTrabix)
                .add(aporteGestionPorTrabix);
    }
}
