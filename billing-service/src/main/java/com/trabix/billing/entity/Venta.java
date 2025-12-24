package com.trabix.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Venta simplificada para billing-service.
 * Solo los campos necesarios para cálculos de cuadre.
 */
@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tanda_id", nullable = false)
    @ToString.Exclude
    private Tanda tanda;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, length = 20)
    private String estado;

    /**
     * Verifica si es un regalo (no genera ingreso).
     */
    public boolean esRegalo() {
        return "REGALO".equals(tipo);
    }

    /**
     * Verifica si está aprobada.
     */
    public boolean estaAprobada() {
        return "APROBADA".equals(estado);
    }

    /**
     * Verifica si genera ingreso (aprobada y no es regalo).
     */
    public boolean generaIngreso() {
        return estaAprobada() && !esRegalo();
    }
}
