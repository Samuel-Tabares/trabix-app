package com.trabix.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.TipoCuadre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con datos de un cuadre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuadreResponse {

    private Long id;
    private TandaInfo tanda;
    private LoteInfo lote;
    private VendedorInfo vendedor;
    
    private TipoCuadre tipo;
    private String estado;
    
    // Montos principales
    private BigDecimal totalRecaudado;
    private BigDecimal montoEsperado;
    private BigDecimal montoRecibido;
    private BigDecimal excedente;
    private BigDecimal excedenteAnterior;
    
    // Distribución
    private BigDecimal montoVendedor;
    private BigDecimal montoCascada;
    private List<DistribucionCascada> distribucionCascada;
    
    // Texto generado
    private String textoWhatsapp;
    
    // Fechas
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaConfirmacion;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TandaInfo {
        private Long id;
        private Integer numero;
        private String descripcion;
        private Integer stockEntregado;
        private Integer stockActual;
        private Double porcentajeRestante;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteInfo {
        private Long id;
        private Integer cantidadTotal;
        private String modelo;
        private BigDecimal inversionTotal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorInfo {
        private Long id;
        private String nombre;
        private String cedula;
        private String nivel;
        private String telefono;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribucionCascada {
        private Long usuarioId;
        private String nombre;
        private String nivel;
        private BigDecimal monto;
        private String porcentaje;
    }
}
