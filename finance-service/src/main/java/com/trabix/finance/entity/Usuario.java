package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para relaciones en finance-service.
 * Solo lectura - los datos se gestionan desde user-service.
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 10)
    private String nivel;

    @Column(nullable = false, length = 20)
    private String rol;

    @Column(nullable = false, length = 20)
    private String estado;

    /**
     * Verifica si el usuario está activo.
     */
    public boolean estaActivo() {
        return "ACTIVO".equals(this.estado);
    }

    /**
     * Verifica si el usuario es admin.
     */
    public boolean esAdmin() {
        return "ADMIN".equals(this.rol);
    }

    /**
     * Verifica si el usuario es vendedor (no admin).
     */
    public boolean esVendedor() {
        return !esAdmin();
    }

    /**
     * Obtiene nombre para mostrar.
     */
    public String getNombreCompleto() {
        return String.format("%s (%s)", this.nombre, this.cedula);
    }
}
