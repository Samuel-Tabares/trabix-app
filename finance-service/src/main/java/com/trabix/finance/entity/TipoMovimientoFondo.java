package com.trabix.finance.entity;

/**
 * Tipos de movimiento del fondo de recompensas.
 */
public enum TipoMovimientoFondo {
    INGRESO("Ingreso al fondo"),
    EGRESO("Egreso del fondo");

    private final String descripcion;

    TipoMovimientoFondo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
