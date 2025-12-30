package com.trabix.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resumen de ventas de un vendedor o tanda.
 * 
 * NOMENCLATURA CORREGIDA:
 * - parteVendedor/parteSamuel: División del recaudado (NO son ganancias hasta recuperar inversión)
 * - gananciaRealVendedor/gananciaRealSamuel: Solo cuando esGanancia=true
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

    // === PARTES (División del recaudado - NO son ganancias hasta recuperar inversión) ===
    
    /**
     * Total parte del vendedor (60% o 50% según modelo).
     * NOTA: NO es ganancia real hasta que se recupere la inversión.
     */
    private BigDecimal totalParteVendedor;
    
    /**
     * Total parte que sube a Samuel (40% o 50% según modelo).
     * NOTA: NO es ganancia real hasta que se recupere la inversión.
     */
    private BigDecimal totalParteSamuel;

    // === GANANCIAS REALES (Solo cuando esGanancia=true) ===
    
    /**
     * Ganancia real del vendedor (solo ventas donde esGanancia=true).
     */
    private BigDecimal gananciaRealVendedor;
    
    /**
     * Ganancia real de Samuel (solo ventas donde esGanancia=true).
     */
    private BigDecimal gananciaRealSamuel;

    // === INVERSIONES ===
    
    /**
     * Inversión total del vendedor en sus lotes activos.
     */
    private BigDecimal inversionVendedor;
    
    /**
     * Inversión total de Samuel en los lotes del vendedor.
     */
    private BigDecimal inversionSamuel;
    
    /**
     * true si ya recuperó su inversión.
     */
    private Boolean inversionVendedorRecuperada;
    
    /**
     * true si Samuel ya recuperó su inversión.
     */
    private Boolean inversionSamuelRecuperada;

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
