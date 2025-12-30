package com.trabix.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request para confirmar un cuadre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCuadreRequest {

    @NotNull(message = "El monto recibido es requerido")
    @PositiveOrZero(message = "El monto debe ser cero o positivo")
    private BigDecimal montoRecibido;

    private String nota;
}
