package com.trabix.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.EstadoTanda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta con datos de una tanda.
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
    private String descripcion; // "Tanda 1 (Inversión)", etc.
    private Integer cantidadAsignada;
    private Integer stockEntregado;
    private Integer stockActual;
    private Integer stockVendido;
    private Double porcentajeRestante;
    private EstadoTanda estado;
    private LocalDateTime fechaLiberacion;
    
    // Info adicional
    private Boolean requiereCuadre; // true si stock <= 20%
    private Boolean puedeSerLiberada;
}
