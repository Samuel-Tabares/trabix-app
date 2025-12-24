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
 * Muestra paso a paso cómo se calculó cada monto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalculoCuadreResponse {

    private TipoCuadre tipo;
    private String modelo; // MODELO_60_40 o MODELO_50_50
    
    // === Datos de entrada ===
    private BigDecimal totalRecaudado;
    private BigDecimal excedenteAnterior;
    private BigDecimal disponibleTotal; // recaudado + excedente anterior
    
    // === Para cuadre de INVERSIÓN (Tanda 1) ===
    private BigDecimal inversionSamuel;
    private BigDecimal inversionVendedor;
    
    // === Para cuadre de GANANCIAS (Tanda 2 y 3) ===
    private BigDecimal gananciasBrutas;
    
    // === Distribución según modelo ===
    
    // Modelo 60/40 (N2)
    private BigDecimal porcentajeVendedor; // 60%
    private BigDecimal porcentajeSamuel;   // 40%
    
    // Modelo 50/50 Cascada (N3+)
    @Builder.Default
    private List<DistribucionNivel> distribucionCascada = new ArrayList<>();
    
    // === Resultado final ===
    private BigDecimal montoQueDebeTransferir;
    private BigDecimal excedenteResultante;
    private BigDecimal montoParaVendedor;
    
    // === Pasos del cálculo (para transparencia) ===
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
