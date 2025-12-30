package com.trabix.notification.entity;

/**
 * Tipos de notificación del sistema.
 */
public enum TipoNotificacion {
    INFO("Información", "info"),
    ALERTA("Alerta", "warning"),
    RECORDATORIO("Recordatorio", "reminder"),
    SISTEMA("Sistema", "system"),
    EXITO("Éxito", "success"),
    ERROR("Error", "error");

    private final String nombre;
    private final String icono;

    TipoNotificacion(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }
}
