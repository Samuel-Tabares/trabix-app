package com.trabix.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notificación del sistema.
 * 
 * Si usuario_id es null, es una notificación broadcast (para todos).
 */
@Entity
@Table(name = "notificaciones", indexes = {
    @Index(name = "idx_notif_usuario", columnList = "usuario_id"),
    @Index(name = "idx_notif_tipo", columnList = "tipo"),
    @Index(name = "idx_notif_leida", columnList = "leida"),
    @Index(name = "idx_notif_usuario_leida", columnList = "usuario_id, leida"),
    @Index(name = "idx_notif_created", columnList = "created_at DESC"),
    @Index(name = "idx_notif_referencia", columnList = "referencia_tipo, referencia_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario destinatario. Si es null = broadcast para todos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoNotificacion tipo = TipoNotificacion.INFO;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    /**
     * Tipo de entidad referenciada (ej: VENTA, TANDA, CUADRE).
     */
    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    /**
     * ID de la entidad referenciada.
     */
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tipo == null) {
            tipo = TipoNotificacion.INFO;
        }
        if (leida == null) {
            leida = false;
        }
    }

    // === Métodos de consulta ===

    public boolean esInfo() {
        return TipoNotificacion.INFO.equals(tipo);
    }

    public boolean esAlerta() {
        return TipoNotificacion.ALERTA.equals(tipo);
    }

    public boolean esRecordatorio() {
        return TipoNotificacion.RECORDATORIO.equals(tipo);
    }

    public boolean esSistema() {
        return TipoNotificacion.SISTEMA.equals(tipo);
    }

    public boolean esExito() {
        return TipoNotificacion.EXITO.equals(tipo);
    }

    public boolean esError() {
        return TipoNotificacion.ERROR.equals(tipo);
    }

    public boolean esBroadcast() {
        return usuario == null;
    }

    public boolean tieneReferencia() {
        return referenciaTipo != null && referenciaId != null;
    }

    // === Métodos de acción ===

    public void marcarLeida() {
        this.leida = true;
        this.fechaLectura = LocalDateTime.now();
    }

    public void marcarNoLeida() {
        this.leida = false;
        this.fechaLectura = null;
    }
}
