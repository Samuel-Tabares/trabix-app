package com.trabix.billing.entity;

import com.trabix.common.enums.TipoCuadre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Cuadre - representa un cuadre de inversión o ganancias.
 * 
 * Tipos de cuadre:
 * - INVERSION: Primer cuadre para recuperar la inversión de Samuel
 * - GANANCIA: Cuadres posteriores para repartir ganancias según modelo
 * 
 * Estados:
 * - PENDIENTE: Cuadre generado, esperando transferencia
 * - EN_PROCESO: Transferencia reportada, esperando confirmación
 * - EXITOSO: Cuadre completado, siguiente tanda puede liberarse
 * - CANCELADO: Cuadre cancelado
 */
@Entity
@Table(name = "cuadres")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuadre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tanda_id", nullable = false)
    @ToString.Exclude
    private Tanda tanda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCuadre tipo;

    @Column(name = "monto_esperado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEsperado;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoRecibido = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal excedente = BigDecimal.ZERO;

    @Column
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "texto_whatsapp", columnDefinition = "TEXT")
    private String textoWhatsapp;

    // === Campos adicionales para el detalle del cuadre ===

    /** Total recaudado en ventas de la tanda */
    @Column(name = "total_recaudado", precision = 12, scale = 2)
    private BigDecimal totalRecaudado;

    /** Monto que corresponde al vendedor */
    @Column(name = "monto_vendedor", precision = 12, scale = 2)
    private BigDecimal montoVendedor;

    /** Monto que sube en cascada (para N3+) */
    @Column(name = "monto_cascada", precision = 12, scale = 2)
    private BigDecimal montoCascada;

    /** Excedente del cuadre anterior (se arrastra) */
    @Column(name = "excedente_anterior", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal excedenteAnterior = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = "PENDIENTE";
        }
        if (montoRecibido == null) {
            montoRecibido = BigDecimal.ZERO;
        }
        if (excedente == null) {
            excedente = BigDecimal.ZERO;
        }
        if (excedenteAnterior == null) {
            excedenteAnterior = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el cuadre como en proceso (transferencia reportada).
     */
    public void marcarEnProceso(BigDecimal montoTransferido) {
        this.estado = "EN_PROCESO";
        this.montoRecibido = montoTransferido;
    }

    /**
     * Confirma el cuadre como exitoso.
     */
    public void confirmar() {
        this.estado = "EXITOSO";
        this.fecha = LocalDateTime.now();
        // Calcular excedente si pagó de más
        if (montoRecibido.compareTo(montoEsperado) > 0) {
            this.excedente = montoRecibido.subtract(montoEsperado);
        }
    }

    /**
     * Cancela el cuadre.
     */
    public void cancelar() {
        this.estado = "CANCELADO";
    }

    /**
     * Verifica si el cuadre está pendiente.
     */
    public boolean estaPendiente() {
        return "PENDIENTE".equals(estado);
    }

    /**
     * Verifica si el cuadre fue exitoso.
     */
    public boolean esExitoso() {
        return "EXITOSO".equals(estado);
    }

    /**
     * Verifica si es cuadre de inversión (tanda 1).
     */
    public boolean esCuadreInversion() {
        return tipo == TipoCuadre.INVERSION;
    }

    /**
     * Verifica si es cuadre de ganancias (tanda 2 o 3).
     */
    public boolean esCuadreGanancias() {
        return tipo == TipoCuadre.GANANCIA;
    }
}
