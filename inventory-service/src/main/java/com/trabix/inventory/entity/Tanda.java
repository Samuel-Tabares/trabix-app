package com.trabix.inventory.entity;

import com.trabix.common.enums.EstadoTanda;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Tanda - una división de un lote.
 * Cada lote tiene 3 tandas: 40%, 30%, 30%.
 * 
 * Flujo:
 * 1. PENDIENTE - Creada pero no liberada
 * 2. LIBERADA - Stock entregado al vendedor
 * 3. EN_CUADRE - Stock <= 20%, esperando transferencia
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Libera la tanda: entrega el stock al vendedor.
     */
    public void liberar() {
        this.stockEntregado = this.cantidadAsignada;
        this.stockActual = this.cantidadAsignada;
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
     * Calcula el porcentaje de stock restante.
     */
    public double getPorcentajeStockRestante() {
        if (stockEntregado == 0) return 100.0;
        return (stockActual * 100.0) / stockEntregado;
    }

    /**
     * Verifica si se debe disparar el cuadre (stock <= 20%).
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

    /**
     * Descripción amigable del número de tanda.
     */
    public String getDescripcion() {
        return switch (numero) {
            case 1 -> "Tanda 1 (Inversión)";
            case 2 -> "Tanda 2 (Recuperación + Ganancia)";
            case 3 -> "Tanda 3 (Ganancia Pura)";
            default -> "Tanda " + numero;
        };
    }
}
