package com.trabix.equipment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Asignación de un kit (nevera + pijama) a un vendedor.
 * 
 * REGLAS:
 * - Nevera y pijama SIEMPRE van juntas
 * - Solo 1 kit por vendedor activo
 * - Mensualidad: $10,000/mes
 * - Se paga primero, luego se asigna
 * - Día de pago = mismo día que pagó primera vez (diaCobroMensual)
 */
@Entity
@Table(name = "asignaciones_equipo", indexes = {
    @Index(name = "idx_asig_usuario", columnList = "usuario_id"),
    @Index(name = "idx_asig_estado", columnList = "estado"),
    @Index(name = "idx_asig_usuario_estado", columnList = "usuario_id, estado")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Vendedor al que se asignó el kit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    /**
     * Fecha de inicio de la asignación.
     */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /**
     * Día del mes en que debe pagar (1-28).
     * Se establece según el día del primer pago.
     */
    @Column(name = "dia_cobro_mensual", nullable = false)
    private Integer diaCobroMensual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoAsignacion estado = EstadoAsignacion.ACTIVO;

    // === Datos del equipo ===

    @Column(name = "numero_serie_nevera", length = 50)
    private String numeroSerieNevera;

    @Column(name = "numero_serie_pijama", length = 50)
    private String numeroSeriePijama;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // === Costos de reposición ===

    @Column(name = "costo_reposicion_nevera", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoReposicionNevera;

    @Column(name = "costo_reposicion_pijama", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoReposicionPijama;

    // === Finalización ===

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cancelacion", length = 30)
    private MotivoCancelacion motivoCancelacion;

    @Column(name = "nota_finalizacion", columnDefinition = "TEXT")
    private String notaFinalizacion;

    /**
     * Indica si ya se pagó la reposición (en caso de pérdida/daño).
     */
    @Column(name = "reposicion_pagada")
    @Builder.Default
    private Boolean reposicionPagada = false;

    // === Pagos ===

    @OneToMany(mappedBy = "asignacion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("anio DESC, mes DESC")
    @ToString.Exclude
    @Builder.Default
    private List<PagoMensualidad> pagos = new ArrayList<>();

    // === Auditoría ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (fechaInicio == null) {
            fechaInicio = now;
        }
        if (estado == null) {
            estado = EstadoAsignacion.ACTIVO;
        }
        if (reposicionPagada == null) {
            reposicionPagada = false;
        }
        // Establecer día de cobro (máximo 28 para evitar problemas con febrero)
        if (diaCobroMensual == null) {
            diaCobroMensual = Math.min(now.getDayOfMonth(), 28);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Métodos de consulta ===

    public boolean estaActivo() {
        return EstadoAsignacion.ACTIVO.equals(estado);
    }

    public boolean estaDevuelto() {
        return EstadoAsignacion.DEVUELTO.equals(estado);
    }

    public boolean estaCancelado() {
        return EstadoAsignacion.CANCELADO.equals(estado);
    }

    public boolean estaSuspendido() {
        return EstadoAsignacion.SUSPENDIDO.equals(estado);
    }

    public boolean estaFinalizado() {
        return estado.estaFinalizado();
    }

    /**
     * Calcula el costo total de reposición del kit.
     */
    public BigDecimal getCostoReposicionTotal() {
        BigDecimal nevera = costoReposicionNevera != null ? costoReposicionNevera : BigDecimal.ZERO;
        BigDecimal pijama = costoReposicionPijama != null ? costoReposicionPijama : BigDecimal.ZERO;
        return nevera.add(pijama);
    }

    /**
     * Calcula el costo de reposición según el motivo de cancelación.
     */
    public BigDecimal calcularCostoReposicionPorMotivo() {
        if (motivoCancelacion == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal costo = BigDecimal.ZERO;
        if (motivoCancelacion.involucraUnNevera()) {
            costo = costo.add(costoReposicionNevera != null ? costoReposicionNevera : BigDecimal.ZERO);
        }
        if (motivoCancelacion.involucraPijama()) {
            costo = costo.add(costoReposicionPijama != null ? costoReposicionPijama : BigDecimal.ZERO);
        }
        return costo;
    }

    // === Métodos de acción ===

    public boolean puedeDevolver() {
        return estaActivo();
    }

    public boolean puedeCancelar() {
        return estaActivo() || estaSuspendido();
    }

    public void devolver() {
        if (!puedeDevolver()) {
            throw new IllegalStateException("No se puede devolver en estado " + estado);
        }
        this.estado = EstadoAsignacion.DEVUELTO;
        this.fechaFinalizacion = LocalDateTime.now();
    }

    public void cancelar(MotivoCancelacion motivo, String nota) {
        if (!puedeCancelar()) {
            throw new IllegalStateException("No se puede cancelar en estado " + estado);
        }
        this.estado = EstadoAsignacion.CANCELADO;
        this.motivoCancelacion = motivo;
        this.notaFinalizacion = nota;
        this.fechaFinalizacion = LocalDateTime.now();
        this.reposicionPagada = false;
    }

    public void suspender() {
        if (!estaActivo()) {
            throw new IllegalStateException("No se puede suspender en estado " + estado);
        }
        this.estado = EstadoAsignacion.SUSPENDIDO;
    }

    public void reactivar() {
        if (!estaSuspendido()) {
            throw new IllegalStateException("Solo se puede reactivar desde estado SUSPENDIDO");
        }
        this.estado = EstadoAsignacion.ACTIVO;
    }

    public void marcarReposicionPagada() {
        this.reposicionPagada = true;
    }
}
