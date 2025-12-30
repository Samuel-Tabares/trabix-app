package com.trabix.inventory.entity;

import com.trabix.common.enums.EstadoLote;
import com.trabix.common.enums.ModeloNegocio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Lote - representa un pedido de granizados de un vendedor.
 * 
 * CORRECCIONES:
 * - @Version para control de concurrencia optimista
 * - Inversiones SIEMPRE 50/50 (independiente del modelo de negocio)
 * - Flags de recuperación de inversión
 * 
 * Cada lote se divide automáticamente en tandas:
 * - < 50 TRABIX = 2 tandas (50% / 50%)
 * - >= 50 TRABIX = 3 tandas (33.3% / 33.3% / 33.3%)
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

    @Version
    private Long version;

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

    // === INVERSIONES (siempre 50/50) ===

    /**
     * Inversión total del lote (cantidad × costo percibido).
     */
    @Column(name = "inversion_total", precision = 12, scale = 2)
    private BigDecimal inversionTotal;

    /**
     * Inversión de Samuel (siempre 50% del lote).
     */
    @Column(name = "inversion_samuel", precision = 12, scale = 2)
    private BigDecimal inversionSamuel;

    /**
     * Inversión del vendedor (siempre 50% del lote).
     */
    @Column(name = "inversion_vendedor", precision = 12, scale = 2)
    private BigDecimal inversionVendedor;

    /**
     * true si Samuel ya recuperó su inversión (Tanda 1 completada).
     */
    @Column(name = "inversion_samuel_recuperada")
    private Boolean inversionSamuelRecuperada;

    /**
     * true si el vendedor ya recuperó su inversión (Tanda 2 completada).
     */
    @Column(name = "inversion_vendedor_recuperada")
    private Boolean inversionVendedorRecuperada;

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
        if (inversionSamuelRecuperada == null) {
            inversionSamuelRecuperada = false;
        }
        if (inversionVendedorRecuperada == null) {
            inversionVendedorRecuperada = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calcula las inversiones (llamar al crear el lote).
     * SIEMPRE 50/50 independiente del modelo de negocio.
     */
    public void calcularInversiones() {
        this.inversionTotal = costoPercibidoUnitario.multiply(BigDecimal.valueOf(cantidadTotal));
        this.inversionSamuel = inversionTotal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        this.inversionVendedor = inversionTotal.subtract(inversionSamuel);
        this.inversionSamuelRecuperada = false;
        this.inversionVendedorRecuperada = false;
    }

    /**
     * Verifica si es modelo 60/40.
     */
    public boolean esModelo60_40() {
        return modelo == ModeloNegocio.MODELO_60_40;
    }

    /**
     * Verifica si es modelo 50/50.
     */
    public boolean esModelo50_50() {
        return modelo == ModeloNegocio.MODELO_50_50;
    }

    /**
     * Obtiene el porcentaje de ganancia del vendedor.
     */
    public int getPorcentajeGananciaVendedor() {
        return esModelo60_40() ? 60 : 50;
    }

    /**
     * Obtiene el porcentaje que sube a Samuel.
     */
    public int getPorcentajeSamuel() {
        return esModelo60_40() ? 40 : 50;
    }

    /**
     * Determina el número de tandas según la cantidad.
     * < 50 trabix = 2 tandas
     * >= 50 trabix = 3 tandas
     */
    public int getNumeroTandas() {
        return cantidadTotal < 50 ? 2 : 3;
    }

    /**
     * Marca la inversión de Samuel como recuperada.
     */
    public void marcarInversionSamuelRecuperada() {
        this.inversionSamuelRecuperada = true;
    }

    /**
     * Marca la inversión del vendedor como recuperada.
     */
    public void marcarInversionVendedorRecuperada() {
        this.inversionVendedorRecuperada = true;
    }

    /**
     * Verifica si ya hay ganancias (ambas inversiones recuperadas).
     */
    public boolean hayGanancias() {
        return Boolean.TRUE.equals(inversionSamuelRecuperada) 
            && Boolean.TRUE.equals(inversionVendedorRecuperada);
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
