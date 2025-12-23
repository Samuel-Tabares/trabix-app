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
    private BigDecimal inversionTotal; // cantidad * costoPercibido
    private ModeloNegocio modelo;
    private EstadoLote estado;
    private LocalDateTime fechaCreacion;
    
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
    }
}
