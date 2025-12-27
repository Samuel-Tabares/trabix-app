package com.trabix.backup.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para backup-service.
 * Solo lectura.
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
    private String rol;

    @Column(nullable = false, length = 20)
    private String estado;

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
