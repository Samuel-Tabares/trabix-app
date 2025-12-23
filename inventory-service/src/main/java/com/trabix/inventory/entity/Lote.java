package com.trabix.inventory.entity;

import com.trabix.common.enums.EstadoLote;
import com.trabix.common.enums.ModeloNegocio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Lote - representa un pedido de granizados de un vendedor.
 * Cada lote se divide automáticamente en 3 tandas (40%, 30%, 30%).
 */
@Entity
@Table(name = "lotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "costo_percibido_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModeloNegocio modelo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("numero ASC")
    @ToString.Exclude
    @Builder.Default
    private List<Tanda> tandas = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoLote.ACTIVO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calcula la inversión total de Samuel en este lote.
     * Inversión = cantidad * costo real (no percibido)
     */
    public BigDecimal calcularInversionSamuel(BigDecimal costoReal) {
        return costoReal.multiply(BigDecimal.valueOf(cantidadTotal));
    }

    /**
     * Calcula la inversión percibida del vendedor.
     * Esto es lo que el vendedor "debe" recuperar primero.
     */
    public BigDecimal calcularInversionVendedor() {
        return costoPercibidoUnitario.multiply(BigDecimal.valueOf(cantidadTotal));
    }

    /**
     * Obtiene el stock total actual sumando todas las tandas liberadas.
     */
    public int getStockActualTotal() {
        return tandas.stream()
                .mapToInt(Tanda::getStockActual)
                .sum();
    }

    /**
     * Obtiene el stock total entregado (tandas liberadas).
     */
    public int getStockEntregadoTotal() {
        return tandas.stream()
                .mapToInt(Tanda::getStockEntregado)
                .sum();
    }

    /**
     * Verifica si el lote está completado (todas las tandas cuadradas).
     */
    public boolean estaCompletado() {
        return tandas.stream()
                .allMatch(t -> "CUADRADA".equals(t.getEstado().name()));
    }
}
