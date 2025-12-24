package com.trabix.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request para confirmar un cuadre (cuando el vendedor transfiere).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCuadreRequest {

    @NotNull(message = "El monto recibido es requerido")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal montoRecibido;

    /**
     * Nota opcional sobre la transferencia.
     */
    private String nota;
}
