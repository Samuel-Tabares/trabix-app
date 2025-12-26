package com.trabix.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.inventory.entity.MovimientoStock.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta con datos de un movimiento de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovimientoStockResponse {

    private Long id;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private Integer stockResultante;
    private BigDecimal costoUnitario;
    private Long loteId;
    private Long usuarioId;
    private String nombreUsuario;
    private String descripcion;
    private LocalDateTime fechaMovimiento;
}
