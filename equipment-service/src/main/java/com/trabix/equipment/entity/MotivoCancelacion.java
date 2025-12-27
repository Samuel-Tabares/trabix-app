package com.trabix.equipment.entity;

/**
 * Motivo de cancelación de una asignación de equipo.
 */
public enum MotivoCancelacion {
    NEVERA_PERDIDA("Nevera perdida"),
    NEVERA_DANADA("Nevera dañada"),
    PIJAMA_PERDIDA("Pijama perdida"),
    PIJAMA_DANADA("Pijama dañada"),
    AMBOS_PERDIDOS("Nevera y pijama perdidos"),
    AMBOS_DANADOS("Nevera y pijama dañados"),
    INCUMPLIMIENTO_PAGO("Incumplimiento de pago prolongado"),
    OTRO("Otro motivo");

    private final String descripcion;

    MotivoCancelacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Indica si este motivo involucra pérdida/daño de la nevera.
     */
    public boolean involucraUnNevera() {
        return this == NEVERA_PERDIDA || this == NEVERA_DANADA || 
               this == AMBOS_PERDIDOS || this == AMBOS_DANADOS;
    }

    /**
     * Indica si este motivo involucra pérdida/daño de la pijama.
     */
    public boolean involucraPijama() {
        return this == PIJAMA_PERDIDA || this == PIJAMA_DANADA || 
               this == AMBOS_PERDIDOS || this == AMBOS_DANADOS;
    }
}
