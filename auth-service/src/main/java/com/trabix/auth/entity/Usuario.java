package com.trabix.auth.entity;

import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad Usuario - representa vendedores, reclutadores y admin.
 * 
 * NIVELES:
 * - N1: Admin (Samuel)
 * - N2: Vendedor directo de Samuel (modelo 60/40)
 * - N3+: Vendedor en cascada (modelo 50/50)
 * 
 * ROLES:
 * - ADMIN: Acceso total
 * - VENDEDOR: Acceso limitado a sus operaciones
 */
@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    /** Nivel en la jerarquía: N1, N2, N3, etc. */
    @Column(nullable = false, length = 10)
    private String nivel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclutador_id")
    @ToString.Exclude
    private Usuario reclutador;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoUsuario estado;

    /** Contador de intentos fallidos de login */
    @Column(name = "intentos_fallidos")
    @Builder.Default
    private Integer intentosFallidos = 0;

    /** Fecha hasta la cual la cuenta está bloqueada */
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    /** Fecha del último login exitoso */
    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaIngreso == null) {
            fechaIngreso = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoUsuario.ACTIVO;
        }
        if (intentosFallidos == null) {
            intentosFallidos = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Métodos de negocio ===

    /**
     * Verifica si la cuenta está temporalmente bloqueada.
     */
    public boolean estaBloqueado() {
        if (bloqueadoHasta == null) return false;
        if (LocalDateTime.now().isAfter(bloqueadoHasta)) {
            // El bloqueo ha expirado
            return false;
        }
        return true;
    }

    /**
     * Incrementa el contador de intentos fallidos.
     */
    public void incrementarIntentosFallidos() {
        this.intentosFallidos = (this.intentosFallidos == null ? 0 : this.intentosFallidos) + 1;
    }

    /**
     * Resetea los intentos fallidos (después de login exitoso).
     */
    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /**
     * Bloquea la cuenta por un período de tiempo.
     */
    public void bloquearCuenta(int minutos) {
        this.bloqueadoHasta = LocalDateTime.now().plusMinutes(minutos);
    }

    /**
     * Registra un login exitoso.
     */
    public void registrarLoginExitoso() {
        this.ultimoLogin = LocalDateTime.now();
        resetearIntentosFallidos();
    }

    /**
     * Determina el modelo de negocio según el nivel.
     */
    public String getModeloNegocio() {
        return "N2".equals(nivel) ? "MODELO_60_40" : "MODELO_50_50";
    }

    /**
     * Verifica si es admin.
     */
    public boolean esAdmin() {
        return rol == RolUsuario.ADMIN || "N1".equals(nivel);
    }

    // === Implementación de UserDetails ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return cedula;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return estado == EstadoUsuario.ACTIVO && !estaBloqueado();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return estado == EstadoUsuario.ACTIVO;
    }
}
