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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentaResponse {

    private Long id;
    private VendedorInfo vendedor;
    private TandaInfo tanda;
    private TipoVenta tipo;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TandaInfo {
        private Long id;
        private Long loteId;
        private Integer numero;
    }
}
