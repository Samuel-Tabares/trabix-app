package com.trabix.sales.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Lote simplificada para relaciones en sales-service.
 * Contiene los campos necesarios para cálculos de ventas.
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

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "costo_percibido_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoUnitario;

    /**
     * Modelo de negocio: MODELO_60_40 o MODELO_50_50
     */
    @Column(nullable = false, length = 20)
    private String modelo;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

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
}
