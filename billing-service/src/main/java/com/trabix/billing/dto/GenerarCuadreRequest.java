package com.trabix.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para generar un cuadre manualmente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarCuadreRequest {

    @NotNull(message = "El ID de la tanda es requerido")
    private Long tandaId;

    private Boolean forzar = false;
}
