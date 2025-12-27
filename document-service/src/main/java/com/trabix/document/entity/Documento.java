package com.trabix.document.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Documento: Cotización o Factura.
 * 
 * Solo el ADMIN puede crear y gestionar documentos.
 * Los items son solo TRABIX (granizados).
 * 
 * Flujo:
 * 1. Crear (BORRADOR)
 * 2. Emitir (genera número, EMITIDO)
 * 3. Pagar (PAGADO) o Anular (ANULADO, solo si no está pagado)
 * 4. Cotizaciones se marcan VENCIDO automáticamente
 */
@Entity
@Table(name = "documentos", indexes = {
    @Index(name = "idx_doc_tipo", columnList = "tipo"),
    @Index(name = "idx_doc_estado", columnList = "estado"),
    @Index(name = "idx_doc_tipo_estado", columnList = "tipo, estado"),
    @Index(name = "idx_doc_numero", columnList = "numero"),
    @Index(name = "idx_doc_fecha_emision", columnList = "fecha_emision"),
    @Index(name = "idx_doc_fecha_vencimiento", columnList = "fecha_vencimiento"),
    @Index(name = "idx_doc_usuario", columnList = "usuario_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumento tipo;

    /**
     * Número único del documento.
     * Se genera al emitir: COT-2025-00001, FAC-2025-00001
     */
    @Column(unique = true, length = 20)
    private String numero;

    /**
     * Usuario que creó el documento (siempre ADMIN).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

    // === Datos del cliente ===
    
    @Column(name = "cliente_nombre", nullable = false, length = 100)
    private String clienteNombre;

    @Column(name = "cliente_telefono", length = 20)
    private String clienteTelefono;

    @Column(name = "cliente_direccion", columnDefinition = "TEXT")
    private String clienteDireccion;

    @Column(name = "cliente_nit", length = 20)
    private String clienteNit;

    @Column(name = "cliente_correo", length = 100)
    private String clienteCorreo;

    // === Items y totales ===
    
    /**
     * Items del documento en formato JSON.
     * Solo TRABIX (granizados).
     */
    @Column(columnDefinition = "JSONB", nullable = false)
    private String items;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // === Fechas y estado ===
    
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    /**
     * Fecha de vencimiento (solo para cotizaciones).
     */
    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @Column(columnDefinition = "TEXT")
    private String notas;

    /**
     * ID de la cotización origen (para facturas convertidas).
     */
    @Column(name = "cotizacion_origen_id")
    private Long cotizacionOrigenId;

    // === Auditoría ===
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (fechaEmision == null) {
            fechaEmision = now;
        }
        if (estado == null) {
            estado = EstadoDocumento.BORRADOR;
        }
        if (iva == null) {
            iva = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Métodos de consulta ===

    public boolean esCotizacion() {
        return TipoDocumento.COTIZACION.equals(tipo);
    }

    public boolean esFactura() {
        return TipoDocumento.FACTURA.equals(tipo);
    }

    public boolean esBorrador() {
        return EstadoDocumento.BORRADOR.equals(estado);
    }

    public boolean estaEmitido() {
        return EstadoDocumento.EMITIDO.equals(estado);
    }

    public boolean estaPagado() {
        return EstadoDocumento.PAGADO.equals(estado);
    }

    public boolean estaAnulado() {
        return EstadoDocumento.ANULADO.equals(estado);
    }

    public boolean estaVencido() {
        return EstadoDocumento.VENCIDO.equals(estado);
    }

    /**
     * Verifica si la cotización está vencida (fecha pasada y no pagada).
     */
    public boolean deberiaMarcarseVencido() {
        return esCotizacion() && 
               estaEmitido() &&
               fechaVencimiento != null && 
               LocalDateTime.now().isAfter(fechaVencimiento);
    }

    // === Métodos de acción ===

    public boolean puedeEditarse() {
        return estado.permiteEdicion();
    }

    public boolean puedeEmitirse() {
        return estado.permiteEmision();
    }

    public boolean puedePagarse() {
        return estado.permitePago();
    }

    public boolean puedeAnularse() {
        return estado.permiteAnulacion();
    }

    public void emitir(String numeroGenerado) {
        if (!puedeEmitirse()) {
            throw new IllegalStateException("El documento no puede emitirse en estado " + estado);
        }
        this.numero = numeroGenerado;
        this.estado = EstadoDocumento.EMITIDO;
    }

    public void marcarPagado() {
        if (!puedePagarse()) {
            throw new IllegalStateException("El documento no puede marcarse como pagado en estado " + estado);
        }
        this.estado = EstadoDocumento.PAGADO;
    }

    public void anular() {
        if (!puedeAnularse()) {
            throw new IllegalStateException("El documento no puede anularse en estado " + estado);
        }
        this.estado = EstadoDocumento.ANULADO;
    }

    public void marcarVencido() {
        if (deberiaMarcarseVencido()) {
            this.estado = EstadoDocumento.VENCIDO;
        }
    }

    /**
     * Verifica si el documento tiene número asignado.
     */
    public boolean tieneNumero() {
        return numero != null && !numero.isBlank();
    }

    /**
     * Verifica si es una factura convertida de cotización.
     */
    public boolean esConvertidaDeCotizacion() {
        return esFactura() && cotizacionOrigenId != null;
    }
}
