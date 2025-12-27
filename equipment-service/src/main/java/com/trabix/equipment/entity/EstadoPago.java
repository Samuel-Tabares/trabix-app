package com.trabix.equipment.entity;

/**
 * Estado del pago de mensualidad.
 */
public enum EstadoPago {
    PENDIENTE("Pendiente", "Pago pendiente"),
    PAGADO("Pagado", "Pago realizado"),
    VENCIDO("Vencido", "Pago vencido sin realizar");

    private final String nombre;
    private final String descripcion;

    EstadoPago(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean estaPendiente() {
        return this == PENDIENTE || this == VENCIDO;
    }
}
