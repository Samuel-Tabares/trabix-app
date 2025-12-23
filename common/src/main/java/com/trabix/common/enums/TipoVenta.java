package com.trabix.common.enums;

/**
 * Tipos de venta disponibles en TRABIX.
 */
public enum TipoVenta {
    UNIDAD,      // $8,000 con licor
    PROMO,       // $12,000 (2x1 = $6,000 c/u)
    SIN_LICOR,   // $7,000
    REGALO,      // $0, máximo 8% del stock
    MAYOR        // Precio variable según cantidad
}
