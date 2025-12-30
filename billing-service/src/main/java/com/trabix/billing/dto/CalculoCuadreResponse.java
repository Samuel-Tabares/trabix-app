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
 * Incluye todos los pasos del cálculo para transparencia.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalculoCuadreResponse {

    private TipoCuadre tipo;
    private String modelo;
    
    /** Total recaudado en la tanda */
    private BigDecimal totalRecaudado;
    
    /** Excedente del cuadre anterior (si aplica) */
    private BigDecimal excedenteAnterior;
    
    /** Total disponible (recaudado + excedente anterior) */
    private BigDecimal disponibleTotal;
    
    /** Inversión de Samuel (50%) - solo para T1 */
    private BigDecimal inversionSamuel;
    
    /** Inversión del vendedor (50%) - para T2 */
    private BigDecimal inversionVendedor;
    
    /** Ganancias brutas (después de inversiones) */
    private BigDecimal gananciasBrutas;
    
    /** Porcentaje de ganancia para vendedor (60 o 50) */
    private BigDecimal porcentajeVendedor;
    
    /** Porcentaje de ganancia para Samuel (40 o 50) */
    private BigDecimal porcentajeSamuel;
    
    /** Distribución en cascada (para modelo 50/50) */
    @Builder.Default
    private List<DistribucionNivel> distribucionCascada = new ArrayList<>();
    
    /** Monto que el vendedor debe transferir a Samuel */
    private BigDecimal montoQueDebeTransferir;
    
    /** Excedente que queda para siguiente cuadre */
    private BigDecimal excedenteResultante;
    
    /** Monto total que queda para el vendedor */
    private BigDecimal montoParaVendedor;
    
    /** Pasos detallados del cálculo */
    @Builder.Default
    private List<String> pasosCalculo = new ArrayList<>();
    
    /**
     * Representa la distribución de un nivel en la cascada.
     */
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
