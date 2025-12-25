package com.trabix.equipment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago de mensualidad por uso de equipo.
 * $10,000 por mes por equipo.
 * Estados: PENDIENTE, PAGADO
 */
@Entity
@Table(name = "pagos_mensualidad", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"equipo_id", "mes", "anio"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoMensualidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    @ToString.Exclude
    private Equipo equipo;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monto = new BigDecimal("10000");

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = "PENDIENTE";
        }
        if (monto == null) {
            monto = new BigDecimal("10000");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equals(estado);
    }

    public boolean estaPagado() {
        return "PAGADO".equals(estado);
    }

    public void marcarPagado() {
        this.estado = "PAGADO";
        this.fechaPago = LocalDateTime.now();
    }

    /**
     * Retorna el nombre del mes.
     */
    public String getNombreMes() {
        String[] meses = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return mes >= 1 && mes <= 12 ? meses[mes] : "Desconocido";
    }

    /**
     * Retorna el período en formato "Mes Año".
     */
    public String getPeriodo() {
        return getNombreMes() + " " + anio;
    }
}
