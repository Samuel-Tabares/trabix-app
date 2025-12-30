package com.trabix.sales.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidad Lote simplificada para relaciones en sales-service.
 * 
 * CORRECCIONES:
 * - @Version para control de concurrencia optimista
 * - inversionTotal, inversionSamuel, inversionVendedor (siempre 50/50)
 * - Flags de recuperación de inversión
 * 
 * IMPORTANTE: La inversión es SIEMPRE 50/50 independiente del modelo de negocio.
 * El modelo (60/40 o 50/50) solo afecta el REPARTO DE GANANCIAS después de recuperar inversiones.
 */
@Entity
@Table(name = "lotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "costo_percibido_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoUnitario;

    /**
     * Modelo de negocio: MODELO_60_40 o MODELO_50_50
     * Solo afecta el REPARTO DE GANANCIAS, NO la inversión.
     */
    @Column(nullable = false, length = 20)
    private String modelo;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

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

    /**
     * Verifica si es modelo 60/40.
     */
    public boolean esModelo60_40() {
        return "MODELO_60_40".equals(modelo);
    }

    /**
     * Verifica si es modelo 50/50.
     */
    public boolean esModelo50_50() {
        return "MODELO_50_50".equals(modelo);
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
     * Calcula las inversiones (llamar al crear el lote).
     */
    public void calcularInversiones() {
        this.inversionTotal = costoPercibidoUnitario.multiply(BigDecimal.valueOf(cantidadTotal));
        this.inversionSamuel = inversionTotal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        this.inversionVendedor = inversionTotal.subtract(inversionSamuel);
        this.inversionSamuelRecuperada = false;
        this.inversionVendedorRecuperada = false;
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
}
