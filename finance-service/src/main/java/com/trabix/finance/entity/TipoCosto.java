package com.trabix.finance.entity;

/**
 * Tipos de costo de producción.
 */
public enum TipoCosto {
    PRODUCCION("Costos de producción directa"),
    INSUMO("Insumos y materiales"),
    MARKETING("Marketing y publicidad"),
    OPERATIVO("Gastos operativos"),
    ENVIO("Costos de envío"),
    OTRO("Otros gastos");

    private final String descripcion;

    TipoCosto(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
