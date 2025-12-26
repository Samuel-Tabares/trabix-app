package com.trabix.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.EstadoVenta;
import com.trabix.common.enums.TipoVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta con datos de una venta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentaResponse {

    private Long id;
    private VendedorInfo vendedor;
    private LoteInfo lote;
    private TandaInfo tanda;
    private TipoVenta tipo;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    
    // === Información de ganancias ===
    
    /**
     * Modelo de negocio aplicado (MODELO_60_40 o MODELO_50_50).
     */
    private String modeloNegocio;
    
    /**
     * Ganancia del vendedor (60% o 50%).
     */
    private BigDecimal gananciaVendedor;
    
    /**
     * Parte que sube a Samuel (40% o 50%).
     */
    private BigDecimal parteSamuel;
    
    private EstadoVenta estado;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaAprobacion;
    private String nota;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorInfo {
        private Long id;
        private String nombre;
        private String cedula;
        private String nivel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteInfo {
        private Long id;
        private Integer cantidadTotal;
        private String modelo;
        private String estado;
        private LocalDateTime fechaCreacion;
        
        /**
         * Stock total disponible en el lote (todas las tandas liberadas).
         */
        private Integer stockDisponible;
        
        /**
         * Porcentaje de ganancia del vendedor (60 o 50).
         */
        private Integer porcentajeGananciaVendedor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TandaInfo {
        private Long id;
        private Integer numero;
        private String descripcion;
        private Integer stockActual;
        private Integer stockEntregado;
        private Double porcentajeRestante;
        private String estado;
        
        /**
         * true si la tanda está cerca del umbral de cuadre.
         */
        private Boolean proximoACuadre;
    }
}
