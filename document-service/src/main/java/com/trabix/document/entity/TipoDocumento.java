package com.trabix.document.entity;

/**
 * Tipos de documento.
 */
public enum TipoDocumento {
    COTIZACION("Cotización", "COT"),
    FACTURA("Factura", "FAC");

    private final String descripcion;
    private final String prefijo;

    TipoDocumento(String descripcion, String prefijo) {
        this.descripcion = descripcion;
        this.prefijo = prefijo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
