package com.trabix.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.EstadoLote;
import com.trabix.common.enums.ModeloNegocio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con datos de un lote.
 * 
 * CORRECCIONES:
 * - Información de inversiones 50/50
 * - Flags de recuperación de inversión
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoteResponse {

    private Long id;
    private VendedorInfo vendedor;
    private Integer cantidadTotal;
    private BigDecimal costoPercibidoUnitario;
    private ModeloNegocio modelo;
    private EstadoLote estado;
    private LocalDateTime fechaCreacion;
    
    // === INVERSIONES (siempre 50/50) ===
    
    /**
     * Inversión total del lote (cantidad × costo percibido).
     */
    private BigDecimal inversionTotal;
    
    /**
     * Inversión de Samuel (siempre 50% del lote).
     */
    private BigDecimal inversionSamuel;
    
    /**
     * Inversión del vendedor (siempre 50% del lote).
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
    
    /**
     * true si ya hay ganancias (ambas inversiones recuperadas).
     */
    private Boolean hayGanancias;
    
    /**
     * Porcentaje de ganancia del vendedor (60 o 50 según modelo).
     */
    private Integer porcentajeGananciaVendedor;
    
    // Resumen de stock
    private Integer stockEntregado;
    private Integer stockActual;
    private Integer stockVendido;
    private Double porcentajeVendido;
    
    // Tandas
    private List<TandaResponse> tandas;

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
}
