package com.trabix.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de cuadres para el panel admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumenCuadresResponse {

    private Integer cuadresPendientes;
    private Integer cuadresEnProceso;
    private Integer cuadresExitosos;
    private Integer cuadresTotales;
    
    private BigDecimal totalEsperado;
    private BigDecimal totalRecibido;
    private BigDecimal totalExcedente;
    
    private List<CuadrePendienteInfo> pendientes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuadrePendienteInfo {
        private Long cuadreId;
        private String vendedorNombre;
        private String vendedorTelefono;
        private Integer tandaNumero;
        private Integer totalTandas;
        private String tipoCuadre;
        private BigDecimal montoEsperado;
        private Double porcentajeStock;
        private String tiempoEspera;
    }
}
