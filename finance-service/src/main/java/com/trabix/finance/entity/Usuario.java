package com.trabix.finance.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para relaciones en finance-service.
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cedula;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String nivel;

    @Column(nullable = false)
    private String rol;

    @Column(nullable = false)
    private String estado;

    /**
     * Verifica si es admin.
     */
    public boolean esAdmin() {
        return "ADMIN".equals(rol) || "N1".equals(nivel);
    }
}
