package com.trabix.document.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Documento: Cotización o Factura.
 * 
 * Tipos: COTIZACION, FACTURA
 * Estados: BORRADOR, EMITIDO, PAGADO, ANULADO, VENCIDO
 */
@Entity
@Table(name = "documentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

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

    @Column(columnDefinition = "JSONB", nullable = false)
    private String items;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "BORRADOR";

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "cotizacion_origen_id")
    private Long cotizacionOrigenId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaEmision == null) {
            fechaEmision = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "BORRADOR";
        }
        if (iva == null) {
            iva = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean esCotizacion() {
        return "COTIZACION".equals(tipo);
    }

    public boolean esFactura() {
        return "FACTURA".equals(tipo);
    }

    public boolean esBorrador() {
        return "BORRADOR".equals(estado);
    }

    public boolean estaEmitido() {
        return "EMITIDO".equals(estado);
    }

    public boolean estaPagado() {
        return "PAGADO".equals(estado);
    }

    public boolean estaAnulado() {
        return "ANULADO".equals(estado);
    }

    public boolean estaVencido() {
        return "VENCIDO".equals(estado) || 
               (esCotizacion() && fechaVencimiento != null && 
                LocalDateTime.now().isAfter(fechaVencimiento) && !estaPagado());
    }

    public void emitir() {
        this.estado = "EMITIDO";
    }

    public void marcarPagado() {
        this.estado = "PAGADO";
    }

    public void anular() {
        this.estado = "ANULADO";
    }
}
