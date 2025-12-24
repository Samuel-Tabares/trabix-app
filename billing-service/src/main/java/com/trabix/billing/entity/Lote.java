package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Lote simplificada para billing-service.
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
     * Calcula la inversión de Samuel (50% de la inversión percibida).
     * Tanto en modelo 60/40 como 50/50, la inversión inicial se divide 50/50.
     */
    public BigDecimal getInversionSamuel() {
        return getInversionPercibidaTotal().divide(BigDecimal.valueOf(2));
    }

    /**
     * Calcula la inversión del vendedor (50% de la inversión percibida).
     */
    public BigDecimal getInversionVendedor() {
        return getInversionPercibidaTotal().divide(BigDecimal.valueOf(2));
    }
}
