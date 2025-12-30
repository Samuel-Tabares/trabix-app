package com.trabix.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para gestionar el stock de producción de Samuel (N1).
 * 
 * Conceptos:
 * - stockProducido: Total de TRABIX producidos históricamente
 * - stockDisponible: TRABIX físicos que Samuel tiene en sus congeladores
 * - stockReservado: TRABIX que debe a los vendedores (suma de tandas pendientes)
 * - deficit: Cuando stockReservado > stockDisponible
 */
@Entity
@Table(name = "stock_produccion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    /**
     * Total de TRABIX producidos históricamente.
     */
    @Column(name = "stock_producido_total", nullable = false)
    private Integer stockProducidoTotal;

    /**
     * TRABIX físicos disponibles en congeladores de Samuel.
     * Este es el stock REAL que tiene.
     */
    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    /**
     * Costo real promedio por TRABIX producido.
     * Varía según costos de insumos.
     */
    @Column(name = "costo_real_unitario", precision = 10, scale = 2)
    private BigDecimal costoRealUnitario;

    /**
     * Fecha de última producción.
     */
    @Column(name = "ultima_produccion")
    private LocalDateTime ultimaProduccion;

    /**
     * Nivel de alerta de stock bajo.
     */
    @Column(name = "nivel_alerta_stock_bajo", nullable = false)
    private Integer nivelAlertaStockBajo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (stockProducidoTotal == null) stockProducidoTotal = 0;
        if (stockDisponible == null) stockDisponible = 0;
        if (nivelAlertaStockBajo == null) nivelAlertaStockBajo = 300;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Registra nueva producción de TRABIX.
     */
    public void registrarProduccion(int cantidad, BigDecimal costoUnitario) {
        this.stockProducidoTotal += cantidad;
        this.stockDisponible += cantidad;
        this.costoRealUnitario = costoUnitario;
        this.ultimaProduccion = LocalDateTime.now();
    }

    /**
     * Entrega TRABIX a un vendedor (reduce stock disponible).
     */
    public void entregarStock(int cantidad) {
        if (cantidad > stockDisponible) {
            throw new IllegalArgumentException(
                "Stock insuficiente. Disponible: " + stockDisponible + ", Solicitado: " + cantidad);
        }
        this.stockDisponible -= cantidad;
    }

    /**
     * Devuelve TRABIX al stock (ej: cancelación de lote).
     */
    public void devolverStock(int cantidad) {
        this.stockDisponible += cantidad;
    }

    /**
     * Verifica si hay stock bajo.
     */
    public boolean tieneStockBajo() {
        return stockDisponible <= nivelAlertaStockBajo;
    }

    /**
     * Calcula el déficit respecto a los reservados.
     * @param totalReservado suma de todos los TRABIX reservados a vendedores
     * @return déficit (positivo si debe, 0 si no)
     */
    public int calcularDeficit(int totalReservado) {
        int deficit = totalReservado - stockDisponible;
        return Math.max(0, deficit);
    }
}
