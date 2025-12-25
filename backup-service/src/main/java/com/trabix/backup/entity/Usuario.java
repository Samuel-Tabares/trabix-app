package com.trabix.backup.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para backup-service.
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

    @Column(nullable = false, unique = true)
    private String cedula;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    private String correo;

    @Column(nullable = false)
    private String nivel;

    @Column(nullable = false)
    private String rol;

    @Column(nullable = false)
    private String estado;

    @Column(name = "reclutado_por_id")
    private Long reclutadoPorId;
}
