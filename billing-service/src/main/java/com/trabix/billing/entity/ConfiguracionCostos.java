package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración de costos del sistema.
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

    public BigDecimal getDiferenciaCosto() {
        return costoPercibidoTrabix.subtract(costoRealTrabix);
    }

    public BigDecimal getMargenTotalPorTrabix() {
        return getDiferenciaCosto()
                .add(aporteFondoPorTrabix)
                .add(aporteGestionPorTrabix);
    }
}
