package com.trabix.sales.dto;

import com.trabix.common.enums.TipoVenta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request para registrar una nueva venta.
 * 
 * Tipos disponibles:
 * - UNIDAD: $8,000 (con licor)
 * - PROMO: $6,000 c/u (2x$12,000, requiere cantidad par)
 * - SIN_LICOR: $7,000
 * - REGALO: $0 (máximo 8% del stock)
 * - MAYOR: Precio variable según cantidad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarVentaRequest {

    @NotNull(message = "El tipo de venta es requerido")
    private TipoVenta tipo;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    /**
     * Precio unitario para ventas al por MAYOR.
     * Solo se usa cuando tipo = MAYOR.
     */
    private BigDecimal precioUnitarioMayor;

    /**
     * Nota opcional (útil para regalos/degustaciones).
     */
    private String nota;

    /**
     * ID de la tanda específica (opcional).
     * Si no se proporciona, se usa la tanda activa del usuario.
     */
    private Long tandaId;
}
