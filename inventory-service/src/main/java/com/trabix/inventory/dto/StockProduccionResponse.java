package com.trabix.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con el estado completo del stock de producción de Samuel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockProduccionResponse {

    // === STOCK DE SAMUEL ===
    
    /**
     * TRABIX físicos que Samuel tiene en sus congeladores.
     */
    private Integer stockDisponible;
    
    /**
     * Total de TRABIX producidos históricamente.
     */
    private Integer stockProducidoTotal;
    
    /**
     * Costo real promedio por TRABIX.
     */
    private BigDecimal costoRealUnitario;
    
    /**
     * Fecha de última producción.
     */
    private LocalDateTime ultimaProduccion;

    // === RESERVADOS (lo que Samuel debe a los vendedores) ===
    
    /**
     * Total de TRABIX reservados a todos los vendedores.
     * Suma de: (cantidadAsignada de tandas PENDIENTES) de todos los lotes activos.
     */
    private Integer totalReservado;
    
    /**
     * Déficit = totalReservado - stockDisponible (si es positivo).
     * Representa cuántos TRABIX Samuel debe producir para cumplir.
     */
    private Integer deficit;
    
    /**
     * Porcentaje de cobertura = (stockDisponible / totalReservado) * 100
     */
    private Double porcentajeCobertura;

    // === ENTREGAS Y VENTAS ===
    
    /**
     * Total de TRABIX entregados a vendedores (tandas liberadas).
     */
    private Integer totalEntregado;
    
    /**
     * Total de TRABIX vendidos por Samuel directamente.
     */
    private Integer totalVentasDirectas;

    // === ALERTAS ===
    
    /**
     * Nivel configurado para alerta de stock bajo.
     */
    private Integer nivelAlertaStockBajo;
    
    /**
     * true si stockDisponible <= nivelAlertaStockBajo
     */
    private Boolean alertaStockBajo;
    
    /**
     * true si hay déficit (totalReservado > stockDisponible)
     */
    private Boolean alertaDeficit;
    
    /**
     * Mensaje de alerta si aplica.
     */
    private String mensajeAlerta;

    // === RESUMEN POR VENDEDOR (opcional) ===
    
    private List<ResumenVendedor> resumenPorVendedor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenVendedor {
        private Long usuarioId;
        private String nombre;
        private String nivel;
        
        /**
         * Total del lote (cantidad pedida).
         */
        private Integer stockPedido;
        
        /**
         * TRABIX que Samuel le tiene guardados (tandas pendientes).
         */
        private Integer stockReservado;
        
        /**
         * TRABIX que el vendedor tiene en su casa (tandas liberadas con stock > 0).
         */
        private Integer stockEnMano;
        
        /**
         * TRABIX vendidos + regalados.
         */
        private Integer stockSalido;
        
        /**
         * Número de lotes activos.
         */
        private Integer lotesActivos;
    }
}
