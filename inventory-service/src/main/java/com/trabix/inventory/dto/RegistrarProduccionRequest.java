package com.trabix.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request para registrar nueva producción de TRABIX.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarProduccionRequest {

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    /**
     * Costo real por TRABIX producido (opcional).
     * Si no se proporciona, se usa el último registrado.
     */
    private BigDecimal costoUnitario;

    /**
     * Nota o descripción del lote de producción.
     */
    private String descripcion;
}
