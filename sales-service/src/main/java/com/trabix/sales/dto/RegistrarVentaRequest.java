package com.trabix.sales.dto;

import com.trabix.common.enums.TipoVenta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para registrar una nueva venta.
 * 
 * Tipos disponibles:
 * - UNIDAD: $8,000 (con licor)
 * - PROMO: $12,000 total (2 unidades x $6,000 c/u)
 * - SIN_LICOR: $7,000
 * - REGALO: $0 (máximo 8% del stock del lote)
 * - MAYOR_CON_LICOR: >20 unidades, precio escalado automático
 * - MAYOR_SIN_LICOR: >20 unidades, precio escalado automático
 * 
 * PRECIOS AL MAYOR (automáticos):
 * | Cantidad | Con Licor | Sin Licor |
 * |----------|-----------|-----------|
 * | 21-49    | $4,900    | $4,800    |
 * | 50-99    | $4,700    | $4,500    |
 * | 100+     | $4,500    | $4,200    |
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
     * Nota opcional (útil para regalos/degustaciones o detalles del cliente).
     */
    private String nota;

    /**
     * ID de la tanda específica (opcional).
     * Si no se proporciona, se usa la tanda activa del usuario (FIFO).
     */
    private Long tandaId;
}
