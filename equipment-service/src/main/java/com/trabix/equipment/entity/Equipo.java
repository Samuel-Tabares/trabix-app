package com.trabix.equipment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Equipo asignado a un vendedor.
 * Tipos: NEVERA, PIJAMA
 * Estados: ACTIVO, DEVUELTO, PERDIDO
 * 
 * Cada equipo genera una mensualidad de $10,000.
 */
@Entity
@Table(name = "equipos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "costo_reposicion", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoReposicion;

    @Column(length = 50)
    private String numeroSerie;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @OneToMany(mappedBy = "equipo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("anio DESC, mes DESC")
    @ToString.Exclude
    @Builder.Default
    private List<PagoMensualidad> pagos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean esNevera() {
        return "NEVERA".equals(tipo);
    }

    public boolean esPijama() {
        return "PIJAMA".equals(tipo);
    }

    public boolean estaActivo() {
        return "ACTIVO".equals(estado);
    }

    public void devolver() {
        this.estado = "DEVUELTO";
        this.fechaDevolucion = LocalDateTime.now();
    }

    public void marcarPerdido() {
        this.estado = "PERDIDO";
    }
}
