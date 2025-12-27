package com.trabix.backup.entity;

/**
 * Estado del backup.
 */
public enum EstadoBackup {
    EN_PROCESO("En proceso", "Backup ejecutándose"),
    COMPLETADO("Completado", "Backup finalizado exitosamente"),
    ERROR("Error", "Backup fallido");

    private final String nombre;
    private final String descripcion;

    EstadoBackup(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
