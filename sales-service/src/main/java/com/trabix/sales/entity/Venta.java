package com.trabix.sales.entity;

import com.trabix.common.enums.EstadoVenta;
import com.trabix.common.enums.TipoVenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Venta - representa una venta de granizados.
 * 
 * Tipos de venta:
 * - UNIDAD: $8,000 (con licor)
 * - PROMO: $6,000 c/u (2x$12,000)
 * - SIN_LICOR: $7,000
 * - REGALO: $0 (degustación/cortesía, máx 8% del stock)
 * - MAYOR: Precio variable según cantidad
 * 
 * Estados:
 * - PENDIENTE: Registrada, esperando aprobación
 * - APROBADA: Confirmada por admin
 * - RECHAZADA: Rechazada por admin (stock restaurado)
 */
@Entity
@Table(name = "ventas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tanda_id", nullable = false)
    @ToString.Exclude
    private Tanda tanda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVenta tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    /**
     * Modelo de negocio aplicado (MODELO_60_40 o MODELO_50_50).
     */
    @Column(name = "modelo_negocio", length = 20)
    private String modeloNegocio;

    /**
     * Ganancia del vendedor (60% o 50% según modelo).
     */
    @Column(name = "ganancia_vendedor", precision = 10, scale = 2)
    private BigDecimal gananciaVendedor;

    /**
     * Parte que sube a Samuel (40% o 50% según modelo).
     */
    @Column(name = "parte_samuel", precision = 10, scale = 2)
    private BigDecimal parteSamuel;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoVenta.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Aprueba la venta.
     */
    public void aprobar() {
        this.estado = EstadoVenta.APROBADA;
        this.fechaAprobacion = LocalDateTime.now();
    }

    /**
     * Rechaza la venta.
     */
    public void rechazar(String motivo) {
        this.estado = EstadoVenta.RECHAZADA;
        this.nota = (this.nota != null ? this.nota + " | " : "") + "Rechazada: " + motivo;
    }

    /**
     * Verifica si es un regalo (sin costo).
     */
    public boolean esRegalo() {
        return tipo == TipoVenta.REGALO;
    }

    /**
     * Verifica si genera ingreso (no es regalo).
     */
    public boolean generaIngreso() {
        return tipo != TipoVenta.REGALO;
    }
}
