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
 * 
 * NOMENCLATURA CORREGIDA:
 * - parteVendedor/parteSamuel: División del recaudado (NO son ganancias hasta recuperar inversión)
 * - esGanancia: true solo cuando AMBAS inversiones (Samuel y vendedor) están recuperadas
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
    
    // === Información de distribución ===
    
    /**
     * Modelo de negocio aplicado (MODELO_60_40 o MODELO_50_50).
     */
    private String modeloNegocio;
    
    /**
     * Parte del vendedor (60% o 50%).
     * NOTA: NO es ganancia real hasta que se recuperen ambas inversiones.
     */
    private BigDecimal parteVendedor;
    
    /**
     * Parte que sube a Samuel (40% o 50%).
     * NOTA: NO es ganancia real hasta que se recuperen ambas inversiones.
     */
    private BigDecimal parteSamuel;
    
    /**
     * true si esta venta ya es ganancia real (ambas inversiones recuperadas).
     */
    private Boolean esGanancia;
    
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
        private String modeloNegocio;
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
        
        // === Información de inversiones ===
        
        /**
         * Inversión total del lote (cantidad × costo percibido).
         */
        private BigDecimal inversionTotal;
        
        /**
         * Inversión de Samuel (50% del lote).
         */
        private BigDecimal inversionSamuel;
        
        /**
         * Inversión del vendedor (50% del lote).
         */
        private BigDecimal inversionVendedor;
        
        /**
         * true si Samuel ya recuperó su inversión.
         */
        private Boolean inversionSamuelRecuperada;
        
        /**
         * true si el vendedor ya recuperó su inversión.
         */
        private Boolean inversionVendedorRecuperada;
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
         * Excedente de dinero de la tanda anterior.
         */
        private BigDecimal excedenteDinero;
        
        /**
         * Excedente de trabix de la tanda anterior.
         */
        private Integer excedenteTrabix;
        
        /**
         * Total recaudado en esta tanda.
         */
        private BigDecimal totalRecaudado;
        
        /**
         * true si la tanda está cerca del umbral de cuadre.
         */
        private Boolean proximoACuadre;
    }
}
