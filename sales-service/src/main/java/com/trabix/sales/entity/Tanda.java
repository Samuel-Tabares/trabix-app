package com.trabix.sales.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad Tanda simplificada para sales-service.
 * 
 * CORRECCIONES:
 * - @Version para control de concurrencia optimista
 * - excedenteDinero: Dinero sobrante de la tanda anterior
 * - excedenteTrabix: Trabix sobrantes de la tanda anterior (se agregan al stock)
 * - totalRecaudado: Acumulado de ventas aprobadas para calcular triggers
 */
@Entity
@Table(name = "tandas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "cantidad_asignada")
    private Integer cantidadAsignada;

    @Column(name = "stock_entregado", nullable = false)
    private Integer stockEntregado;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private String estado;

    /**
     * Excedente de dinero de la tanda anterior.
     * Se usa como saldo inicial para esta tanda.
     */
    @Column(name = "excedente_dinero", precision = 12, scale = 2)
    private BigDecimal excedenteDinero;

    /**
     * Excedente de trabix de la tanda anterior.
     * Se agregan al stockActual de esta tanda.
     */
    @Column(name = "excedente_trabix")
    private Integer excedenteTrabix;

    /**
     * Total recaudado en esta tanda (ventas aprobadas).
     * Se usa para calcular triggers de cuadre.
     */
    @Column(name = "total_recaudado", precision = 12, scale = 2)
    private BigDecimal totalRecaudado;

    /**
     * Reduce el stock de la tanda.
     */
    public void reducirStock(int cantidad) {
        if (cantidad > stockActual) {
            throw new IllegalArgumentException("Stock insuficiente. Disponible: " + stockActual);
        }
        this.stockActual -= cantidad;
    }

    /**
     * Restaura stock (cuando se rechaza una venta).
     */
    public void restaurarStock(int cantidad) {
        this.stockActual += cantidad;
    }

    /**
     * Agrega excedente de trabix de la tanda anterior al stock.
     */
    public void agregarExcedenteTrabix(int cantidad) {
        if (this.excedenteTrabix == null) {
            this.excedenteTrabix = 0;
        }
        this.excedenteTrabix += cantidad;
        this.stockActual += cantidad;
        this.stockEntregado += cantidad;
    }

    /**
     * Registra excedente de dinero de la tanda anterior.
     */
    public void agregarExcedenteDinero(BigDecimal monto) {
        if (this.excedenteDinero == null) {
            this.excedenteDinero = BigDecimal.ZERO;
        }
        this.excedenteDinero = this.excedenteDinero.add(monto);
    }

    /**
     * Agrega al total recaudado (cuando se aprueba una venta).
     */
    public void agregarRecaudado(BigDecimal monto) {
        if (this.totalRecaudado == null) {
            this.totalRecaudado = BigDecimal.ZERO;
        }
        this.totalRecaudado = this.totalRecaudado.add(monto);
    }

    /**
     * Calcula el porcentaje de stock restante.
     */
    public double getPorcentajeStockRestante() {
        if (stockEntregado == 0) return 100.0;
        return (stockActual * 100.0) / stockEntregado;
    }
}
