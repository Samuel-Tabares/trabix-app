package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Usuario simplificada para relaciones en billing-service.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclutador_id")
    @ToString.Exclude
    private Usuario reclutador;

    /**
     * Determina el modelo de negocio según el nivel.
     * N2 = 60/40 (directo con admin)
     * N3+ = 50/50 (cascada)
     */
    public String getModeloNegocio() {
        return "N2".equals(nivel) ? "MODELO_60_40" : "MODELO_50_50";
    }

    /**
     * Verifica si es admin (N1).
     */
    public boolean esAdmin() {
        return "N1".equals(nivel) || "ADMIN".equals(rol);
    }
}
