package com.trabix.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de refresh para renovar access tokens sin re-login.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revocado = false;

    /** IP desde donde se generó el token */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** User-Agent del dispositivo */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (revocado == null) {
            revocado = false;
        }
    }

    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    /**
     * Verifica si el token es válido (no revocado y no expirado).
     */
    public boolean isValido() {
        return !revocado && !isExpirado();
    }

    /**
     * Revoca el token.
     */
    public void revocar() {
        this.revocado = true;
    }
}
