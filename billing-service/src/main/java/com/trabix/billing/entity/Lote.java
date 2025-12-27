package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Lote para billing-service.
 * 
 * INVERSIÓN: SIEMPRE 50/50 (Samuel y vendedor ponen mitad y mitad)
 * 
 * GANANCIAS (según modelo):
 * - MODELO_60_40 (N2): 60% vendedor, 40% Samuel
 * - MODELO_50_50 (N3+): 50% vendedor, 50% cascada
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "costo_percibido_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPercibidoUnitario;

    @Column(nullable = false, length = 20)
    private String modelo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false, length = 20)
    private String estado;

    @OneToMany(mappedBy = "lote", fetch = FetchType.LAZY)
    @OrderBy("numero ASC")
    @ToString.Exclude
    private List<Tanda> tandas = new ArrayList<>();

    /**
     * Calcula la inversión total percibida del lote.
     */
    public BigDecimal getInversionPercibidaTotal() {
        return costoPercibidoUnitario.multiply(BigDecimal.valueOf(cantidadTotal));
    }

    /**
     * Calcula la inversión de Samuel.
     * SIEMPRE 50% - no depende del modelo.
     */
    public BigDecimal getInversionSamuel() {
        return getInversionPercibidaTotal()
                .multiply(new BigDecimal("0.50"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la inversión del vendedor.
     * SIEMPRE 50% - no depende del modelo.
     */
    public BigDecimal getInversionVendedor() {
        return getInversionPercibidaTotal()
                .multiply(new BigDecimal("0.50"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Porcentaje de inversión de Samuel (siempre 50).
     */
    public int getPorcentajeInversionSamuel() {
        return 50;
    }

    /**
     * Porcentaje de inversión del vendedor (siempre 50).
     */
    public int getPorcentajeInversionVendedor() {
        return 50;
    }

    /**
     * Porcentaje de ganancia del vendedor en cuadres de ganancias.
     * - MODELO_60_40: 60% para vendedor
     * - MODELO_50_50: 50% para vendedor (cascada)
     */
    public int getPorcentajeGananciaVendedor() {
        return "MODELO_60_40".equals(modelo) ? 60 : 50;
    }

    /**
     * Porcentaje que sube a Samuel en cuadres de ganancias.
     */
    public int getPorcentajeGananciaSamuel() {
        return "MODELO_60_40".equals(modelo) ? 40 : 50;
    }

    /**
     * Número de tandas según cantidad de TRABIX.
     * <= 50 = 2 tandas, > 50 = 3 tandas
     */
    public int getNumeroTandas() {
        return cantidadTotal <= 50 ? 2 : 3;
    }

    /**
     * Verifica si es modelo 60/40 (N2).
     */
    public boolean esModelo60_40() {
        return "MODELO_60_40".equals(modelo);
    }

    /**
     * Verifica si es modelo 50/50 cascada (N3+).
     */
    public boolean esModelo50_50() {
        return "MODELO_50_50".equals(modelo);
    }
}
