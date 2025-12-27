package com.trabix.equipment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pago de mensualidad por uso del kit (nevera + pijama).
 * 
 * $10,000 por mes por el kit completo.
 * 
 * IMPORTANTE:
 * - Pagos pendientes bloquean el flujo de cuadres del vendedor
 * - La fecha de vencimiento es el día de cobro del mes correspondiente
 */
@Entity
@Table(name = "pagos_mensualidad", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_pago_asignacion_mes_anio",
           columnNames = {"asignacion_id", "mes", "anio"}
       ),
       indexes = {
           @Index(name = "idx_pago_asignacion", columnList = "asignacion_id"),
           @Index(name = "idx_pago_estado", columnList = "estado"),
           @Index(name = "idx_pago_mes_anio", columnList = "mes, anio"),
           @Index(name = "idx_pago_fecha_vencimiento", columnList = "fecha_vencimiento")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoMensualidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false)
    @ToString.Exclude
    private AsignacionEquipo asignacion;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monto = new BigDecimal("10000");

    /**
     * Fecha en que vence este pago.
     */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (estado == null) {
            estado = EstadoPago.PENDIENTE;
        }
        if (monto == null) {
            monto = new BigDecimal("10000");
        }
        // Calcular fecha de vencimiento si no está definida
        if (fechaVencimiento == null && asignacion != null) {
            int dia = asignacion.getDiaCobroMensual() != null ? 
                    asignacion.getDiaCobroMensual() : 1;
            // Ajustar día si es mayor que los días del mes
            int maxDia = LocalDate.of(anio, mes, 1).lengthOfMonth();
            dia = Math.min(dia, maxDia);
            fechaVencimiento = LocalDate.of(anio, mes, dia);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Métodos de consulta ===

    public boolean estaPendiente() {
        return EstadoPago.PENDIENTE.equals(estado);
    }

    public boolean estaPagado() {
        return EstadoPago.PAGADO.equals(estado);
    }

    public boolean estaVencido() {
        return EstadoPago.VENCIDO.equals(estado);
    }

    /**
     * Verifica si el pago debería marcarse como vencido.
     */
    public boolean deberiaMarcarseVencido() {
        return estaPendiente() && 
               fechaVencimiento != null && 
               LocalDate.now().isAfter(fechaVencimiento);
    }

    /**
     * Verifica si el pago requiere atención (pendiente o vencido).
     */
    public boolean requiereAtencion() {
        return estado.estaPendiente();
    }

    // === Métodos de acción ===

    public void marcarPagado() {
        this.estado = EstadoPago.PAGADO;
        this.fechaPago = LocalDateTime.now();
    }

    public void marcarPagado(String nota) {
        marcarPagado();
        this.nota = nota;
    }

    public void marcarVencido() {
        if (estaPendiente()) {
            this.estado = EstadoPago.VENCIDO;
        }
    }

    // === Métodos de formato ===

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

    /**
     * Valida que mes y año sean válidos.
     */
    public boolean esPeriodoValido() {
        return mes != null && mes >= 1 && mes <= 12 && 
               anio != null && anio >= 2020 && anio <= 2100;
    }
}
