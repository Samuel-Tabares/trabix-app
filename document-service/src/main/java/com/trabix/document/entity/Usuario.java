package com.trabix.document.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para relaciones en document-service.
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

    public boolean estaActivo() {
        return "ACTIVO".equals(this.estado);
    }

    public boolean esAdmin() {
        return "ADMIN".equals(this.rol);
    }
}
