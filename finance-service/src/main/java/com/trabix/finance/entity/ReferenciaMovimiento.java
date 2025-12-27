package com.trabix.finance.entity;

/**
 * Tipos de referencia para movimientos del fondo.
 * Indica el origen o destino del movimiento.
 */
public enum ReferenciaMovimiento {
    PAGO_LOTE("Pago de lote por vendedor"),
    PREMIO("Premio a vendedor"),
    INCENTIVO("Incentivo por meta"),
    BONIFICACION("Bonificación especial"),
    AJUSTE("Ajuste contable"),
    RETIRO("Retiro administrativo"),
    OTRO("Otro tipo de movimiento");

    private final String descripcion;

    ReferenciaMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
