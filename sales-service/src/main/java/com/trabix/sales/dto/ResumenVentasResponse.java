package com.trabix.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resumen de ventas de un vendedor o tanda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumenVentasResponse {

    // Totales generales
    private Integer totalVentas;
    private Integer totalUnidadesVendidas;
    private BigDecimal totalRecaudado;

    // Por tipo de venta
    private Integer ventasUnidad;
    private Integer unidadesUnidad;
    private BigDecimal recaudadoUnidad;

    private Integer ventasPromo;
    private Integer unidadesPromo;
    private BigDecimal recaudadoPromo;

    private Integer ventasSinLicor;
    private Integer unidadesSinLicor;
    private BigDecimal recaudadoSinLicor;

    private Integer ventasRegalo;
    private Integer unidadesRegalo;

    private Integer ventasMayor;
    private Integer unidadesMayor;
    private BigDecimal recaudadoMayor;

    // Por estado
    private Integer ventasPendientes;
    private Integer ventasAprobadas;
    private Integer ventasRechazadas;

    // Información adicional
    private BigDecimal promedioVenta;
    private BigDecimal precioPromedioUnitario;
}
