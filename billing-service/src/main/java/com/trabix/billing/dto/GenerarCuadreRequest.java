package com.trabix.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para generar un cuadre manualmente.
 * Normalmente los cuadres se generan automáticamente cuando stock <= 20%,
 * pero el admin puede forzar un cuadre si lo necesita.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarCuadreRequest {

    @NotNull(message = "El ID de la tanda es requerido")
    private Long tandaId;

    /**
     * Si es true, genera el cuadre aunque el stock no esté en 20%.
     * Solo para casos especiales.
     */
    private Boolean forzar = false;
}
