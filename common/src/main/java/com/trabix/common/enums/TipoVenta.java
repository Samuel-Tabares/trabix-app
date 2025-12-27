package com.trabix.common.enums;

/**
 * Tipos de venta disponibles en TRABIX.
 * 
 * PRECIOS:
 * - UNIDAD: $8,000 (con licor)
 * - PROMO: $12,000 total (2 unidades, $6,000 c/u)
 * - SIN_LICOR: $7,000
 * - REGALO: $0 (máximo 8% del stock del lote)
 * - MAYOR_CON_LICOR: >20 unidades, precio escalado
 *   - 21-49: $4,900
 *   - 50-99: $4,700
 *   - 100+:  $4,500
 * - MAYOR_SIN_LICOR: >20 unidades, precio escalado
 *   - 21-49: $4,800
 *   - 50-99: $4,500
 *   - 100+:  $4,200
 */
public enum TipoVenta {
    UNIDAD,           // $8,000 con licor
    PROMO,            // $12,000 (2x1 = $6,000 c/u)
    SIN_LICOR,        // $7,000
    REGALO,           // $0, máximo 8% del stock
    MAYOR_CON_LICOR,  // >20 unidades, precio escalado con licor
    MAYOR_SIN_LICOR;  // >20 unidades, precio escalado sin licor

    /**
     * Verifica si es una venta al por mayor.
     */
    public boolean esMayor() {
        return this == MAYOR_CON_LICOR || this == MAYOR_SIN_LICOR;
    }

    /**
     * Verifica si es una venta con licor.
     */
    public boolean conLicor() {
        return this == UNIDAD || this == PROMO || this == MAYOR_CON_LICOR;
    }
}
