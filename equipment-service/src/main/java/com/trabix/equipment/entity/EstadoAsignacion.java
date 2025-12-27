package com.trabix.equipment.entity;

/**
 * Estado de la asignación del kit (nevera + pijama).
 */
public enum EstadoAsignacion {
    ACTIVO("Activo", "Kit asignado y en uso"),
    DEVUELTO("Devuelto", "Kit devuelto al stock"),
    CANCELADO("Cancelado", "Asignación cancelada por pérdida/daño"),
    SUSPENDIDO("Suspendido", "Suspendido por falta de pago");

    private final String nombre;
    private final String descripcion;

    EstadoAsignacion(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean permiteOperaciones() {
        return this == ACTIVO;
    }

    public boolean estaFinalizado() {
        return this == DEVUELTO || this == CANCELADO;
    }
}
