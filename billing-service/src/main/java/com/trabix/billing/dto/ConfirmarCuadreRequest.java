package com.trabix.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal montoRecibido;

    private String nota;
}
