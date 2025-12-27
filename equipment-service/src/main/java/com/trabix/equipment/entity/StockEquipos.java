package com.trabix.equipment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stock de equipos (kits nevera + pijama) disponibles.
 * 
 * Esta es una entidad singleton - solo debe existir un registro.
 * 
 * El stock funciona así:
 * - Admin compra kits y los agrega
 * - Al asignar a vendedor: disponibles - 1
 * - Al devolver: disponibles + 1
 * - Al reponer (después de pérdida pagada): disponibles + 1
 */
@Entity
@Table(name = "stock_equipos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockEquipos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Cantidad de kits disponibles para asignar.
     */
    @Column(name = "kits_disponibles", nullable = false)
    @Builder.Default
    private Integer kitsDisponibles = 0;

    /**
     * Total histórico de kits (para referencia).
     */
    @Column(name = "total_kits_historico", nullable = false)
    @Builder.Default
    private Integer totalKitsHistorico = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (kitsDisponibles == null) {
            kitsDisponibles = 0;
        }
        if (totalKitsHistorico == null) {
            totalKitsHistorico = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Métodos de consulta ===

    public boolean hayDisponibles() {
        return kitsDisponibles != null && kitsDisponibles > 0;
    }

    public boolean hayDisponibles(int cantidad) {
        return kitsDisponibles != null && kitsDisponibles >= cantidad;
    }

    // === Métodos de acción ===

    /**
     * Agrega kits al stock (cuando admin compra nuevos).
     */
    public void agregarKits(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        this.kitsDisponibles += cantidad;
        this.totalKitsHistorico += cantidad;
    }

    /**
     * Retira un kit del stock (cuando se asigna a vendedor).
     */
    public void retirarKit() {
        if (!hayDisponibles()) {
            throw new IllegalStateException("No hay kits disponibles en stock");
        }
        this.kitsDisponibles--;
    }

    /**
     * Devuelve un kit al stock (cuando vendedor lo devuelve o se repone).
     */
    public void devolverKit() {
        this.kitsDisponibles++;
    }

    /**
     * Ajusta el stock manualmente (para correcciones).
     */
    public void ajustarStock(int nuevoValor) {
        if (nuevoValor < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.kitsDisponibles = nuevoValor;
    }
}
