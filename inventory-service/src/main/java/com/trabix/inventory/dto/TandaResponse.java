package com.trabix.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.EstadoTanda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta con datos de una tanda.
 * 
 * CORRECCIONES:
 * - Excedentes de tanda anterior
 * - Total recaudado para triggers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TandaResponse {

    private Long id;
    private Long loteId;
    private Integer numero;
    private String descripcion; // "Tanda 1 (Recuperar inversión Samuel)", etc.
    private Integer cantidadAsignada;
    private Integer stockEntregado;
    private Integer stockActual;
    private Integer stockVendido;
    private Double porcentajeRestante;
    private EstadoTanda estado;
    private LocalDateTime fechaLiberacion;
    
    // === EXCEDENTES DE TANDA ANTERIOR ===
    
    /**
     * Excedente de dinero de la tanda anterior.
     */
    private BigDecimal excedenteDinero;
    
    /**
     * Excedente de trabix de la tanda anterior.
     */
    private Integer excedenteTrabix;
    
    /**
     * Total recaudado en esta tanda (ventas aprobadas).
     */
    private BigDecimal totalRecaudado;
    
    // Info adicional
    
    /**
     * true si stock <= umbral de cuadre.
     */
    private Boolean requiereCuadre;
    
    /**
     * true si puede ser liberada (tanda anterior cuadrada).
     */
    private Boolean puedeSerLiberada;
    
    /**
     * true si está cerca del umbral de cuadre.
     */
    private Boolean proximoACuadre;
}
