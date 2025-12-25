package com.trabix.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notificación del sistema.
 * 
 * Tipos: INFO, ALERTA, RECORDATORIO, SISTEMA, EXITO, ERROR
 * 
 * Si usuario_id es null, es una notificación broadcast (para todos).
 */
@Entity
@Table(name = "notificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "INFO";

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tipo == null) {
            tipo = "INFO";
        }
        if (leida == null) {
            leida = false;
        }
    }

    public boolean esInfo() {
        return "INFO".equals(tipo);
    }

    public boolean esAlerta() {
        return "ALERTA".equals(tipo);
    }

    public boolean esRecordatorio() {
        return "RECORDATORIO".equals(tipo);
    }

    public boolean esSistema() {
        return "SISTEMA".equals(tipo);
    }

    public boolean esBroadcast() {
        return usuario == null;
    }

    public void marcarLeida() {
        this.leida = true;
        this.fechaLectura = LocalDateTime.now();
    }

    public void marcarNoLeida() {
        this.leida = false;
        this.fechaLectura = null;
    }
}
