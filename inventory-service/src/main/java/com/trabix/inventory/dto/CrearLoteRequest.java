package com.trabix.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear un nuevo lote de granizados.
 * 
 * Reglas:
 * - < 50 TRABIX = 2 tandas (50% / 50%)
 * - >= 50 TRABIX = 3 tandas (33.3% / 33.3% / 33.3%)
 * - Inversión SIEMPRE 50/50 entre Samuel y vendedor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearLoteRequest {

    @NotNull(message = "El ID del vendedor es requerido")
    private Long usuarioId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 10, message = "El lote mínimo es de 10 unidades")
    private Integer cantidad;

    /**
     * Costo percibido por unidad (opcional, default $2,400).
     * Este es el "costo" que el vendedor ve y debe recuperar.
     */
    private Double costoPercibidoUnitario;
}
