package com.trabix.inventory.entity;

import com.trabix.common.enums.EstadoTanda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Tanda - una división de un lote.
 * 
 * CORRECCIONES:
 * - @Version para control de concurrencia optimista
 * - excedenteDinero: Dinero sobrante de la tanda anterior
 * - excedenteTrabix: Trabix sobrantes de la tanda anterior (se agregan al stock)
 * - totalRecaudado: Acumulado de ventas aprobadas para calcular triggers
 * 
 * Distribución de tandas:
 * - < 50 TRABIX = 2 tandas (50% / 50%)
 * - >= 50 TRABIX = 3 tandas (33.3% / 33.3% / 33.3%)
 * 
 * Flujo:
 * 1. PENDIENTE - Creada pero no liberada
 * 2. LIBERADA - Stock entregado al vendedor
 * 3. EN_CUADRE - Stock <= umbral%, esperando transferencia
 * 4. CUADRADA - Cuadre exitoso, siguiente tanda liberada
 */
@Entity
@Table(name = "tandas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    @ToString.Exclude
    private Lote lote;

    @Column(nullable = false)
    private Integer numero; // 1, 2 o 3

    @Column(name = "cantidad_asignada", nullable = false)
    private Integer cantidadAsignada;

    @Column(name = "stock_entregado", nullable = false)
    private Integer stockEntregado;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTanda estado;

    // === EXCEDENTES DE TANDA ANTERIOR ===

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoTanda.PENDIENTE;
        }
        if (stockEntregado == null) {
            stockEntregado = 0;
        }
        if (stockActual == null) {
            stockActual = 0;
        }
        if (excedenteDinero == null) {
            excedenteDinero = BigDecimal.ZERO;
        }
        if (excedenteTrabix == null) {
            excedenteTrabix = 0;
        }
        if (totalRecaudado == null) {
            totalRecaudado = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Libera la tanda: entrega el stock al vendedor.
     */
    public void liberar() {
        this.stockEntregado = this.cantidadAsignada + (this.excedenteTrabix != null ? this.excedenteTrabix : 0);
        this.stockActual = this.stockEntregado;
        this.fechaLiberacion = LocalDateTime.now();
        this.estado = EstadoTanda.LIBERADA;
    }

    /**
     * Reduce el stock (por una venta).
     */
    public void reducirStock(int cantidad) {
        if (cantidad > stockActual) {
            throw new IllegalArgumentException("No hay suficiente stock. Disponible: " + stockActual);
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

    /**
     * Verifica si se debe disparar el cuadre (stock <= porcentaje).
     */
    public boolean debeTriggerCuadre(int porcentajeTrigger) {
        return estado == EstadoTanda.LIBERADA && getPorcentajeStockRestante() <= porcentajeTrigger;
    }

    /**
     * Verifica si esta tanda puede ser liberada.
     * Tanda 1 siempre puede. Tanda 2 y 3 requieren cuadre previo.
     */
    public boolean puedeSerLiberada() {
        if (numero == 1) {
            return estado == EstadoTanda.PENDIENTE;
        }
        // Para tanda 2 y 3, verificar que la anterior esté cuadrada
        // Esto se valida en el servicio con acceso al lote completo
        return estado == EstadoTanda.PENDIENTE;
    }

    /**
     * Marca la tanda como en proceso de cuadre.
     */
    public void iniciarCuadre() {
        this.estado = EstadoTanda.EN_CUADRE;
    }

    /**
     * Marca la tanda como cuadrada exitosamente.
     */
    public void completarCuadre() {
        this.estado = EstadoTanda.CUADRADA;
    }
}
