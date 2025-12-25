package com.trabix.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.TipoCuadre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Detalle del cálculo de un cuadre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalculoCuadreResponse {

    private TipoCuadre tipo;
    private String modelo;
    
    private BigDecimal totalRecaudado;
    private BigDecimal excedenteAnterior;
    private BigDecimal disponibleTotal;
    
    private BigDecimal inversionSamuel;
    private BigDecimal inversionVendedor;
    
    private BigDecimal gananciasBrutas;
    
    private BigDecimal porcentajeVendedor;
    private BigDecimal porcentajeSamuel;
    
    @Builder.Default
    private List<DistribucionNivel> distribucionCascada = new ArrayList<>();
    
    private BigDecimal montoQueDebeTransferir;
    private BigDecimal excedenteResultante;
    private BigDecimal montoParaVendedor;
    
    @Builder.Default
    private List<String> pasosCalculo = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribucionNivel {
        private String nivel;
        private String nombre;
        private BigDecimal porcentaje;
        private BigDecimal monto;
        private String explicacion;
    }
}
