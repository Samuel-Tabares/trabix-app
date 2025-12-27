package com.trabix.document.entity;

/**
 * Estados de documento.
 * 
 * Flujo:
 * BORRADOR → EMITIDO → PAGADO
 *                   → ANULADO (si no está pagado)
 *         → VENCIDO (solo cotizaciones, automático)
 */
public enum EstadoDocumento {
    BORRADOR("Borrador", "Documento en edición, sin número"),
    EMITIDO("Emitido", "Documento emitido con número, pendiente de pago"),
    PAGADO("Pagado", "Documento pagado"),
    ANULADO("Anulado", "Documento anulado"),
    VENCIDO("Vencido", "Cotización vencida sin pago");

    private final String nombre;
    private final String descripcion;

    EstadoDocumento(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Verifica si el documento puede ser editado.
     */
    public boolean permiteEdicion() {
        return this == BORRADOR;
    }

    /**
     * Verifica si el documento puede ser emitido.
     */
    public boolean permiteEmision() {
        return this == BORRADOR;
    }

    /**
     * Verifica si el documento puede marcarse como pagado.
     */
    public boolean permitePago() {
        return this == EMITIDO;
    }

    /**
     * Verifica si el documento puede ser anulado.
     * NO se puede anular un documento PAGADO.
     */
    public boolean permiteAnulacion() {
        return this != PAGADO && this != ANULADO;
    }
}
