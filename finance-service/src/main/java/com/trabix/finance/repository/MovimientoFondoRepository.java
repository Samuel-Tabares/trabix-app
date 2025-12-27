package com.trabix.finance.repository;

import com.trabix.finance.entity.MovimientoFondo;
import com.trabix.finance.entity.ReferenciaMovimiento;
import com.trabix.finance.entity.TipoMovimientoFondo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoFondoRepository extends JpaRepository<MovimientoFondo, Long> {

    // === Consultas paginadas ===
    
    Page<MovimientoFondo> findByFondoId(Long fondoId, Pageable pageable);
    
    Page<MovimientoFondo> findByTipo(TipoMovimientoFondo tipo, Pageable pageable);
    
    Page<MovimientoFondo> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    // === Consultas por beneficiario ===
    
    List<MovimientoFondo> findByBeneficiarioIdOrderByFechaDesc(Long beneficiarioId);
    
    Page<MovimientoFondo> findByBeneficiarioId(Long beneficiarioId, Pageable pageable);

    // === Consultas por vendedor origen ===
    
    List<MovimientoFondo> findByVendedorOrigenIdOrderByFechaDesc(Long vendedorId);

    // === Consultas por período ===
    
    List<MovimientoFondo> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    // === Sumas por tipo ===
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = :tipo")
    BigDecimal sumarPorTipo(@Param("tipo") TipoMovimientoFondo tipo);
    
    default BigDecimal sumarTotalIngresos() {
        return sumarPorTipo(TipoMovimientoFondo.INGRESO);
    }
    
    default BigDecimal sumarTotalEgresos() {
        return sumarPorTipo(TipoMovimientoFondo.EGRESO);
    }

    // === Sumas por período ===
    
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = :tipo AND m.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarPorTipoYPeriodo(
            @Param("tipo") TipoMovimientoFondo tipo,
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta
    );
    
    default BigDecimal sumarIngresosPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return sumarPorTipoYPeriodo(TipoMovimientoFondo.INGRESO, desde, hasta);
    }
    
    default BigDecimal sumarEgresosPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return sumarPorTipoYPeriodo(TipoMovimientoFondo.EGRESO, desde, hasta);
    }

    // === PREMIOS por beneficiario (CORREGIDO - solo cuenta premios reales) ===
    
    /**
     * Suma solo los PREMIOS de un beneficiario.
     * Un premio es un EGRESO con referenciaTipo = PREMIO y beneficiario asignado.
     */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.beneficiario.id = :beneficiarioId 
        AND m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.EGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PREMIO
        """)
    BigDecimal sumarPremiosPorBeneficiario(@Param("beneficiarioId") Long beneficiarioId);
    
    /**
     * Cuenta solo los PREMIOS de un beneficiario (CORREGIDO).
     */
    @Query("""
        SELECT COUNT(m) FROM MovimientoFondo m 
        WHERE m.beneficiario.id = :beneficiarioId 
        AND m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.EGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PREMIO
        """)
    long contarPremiosPorBeneficiario(@Param("beneficiarioId") Long beneficiarioId);

    // === Ingresos por vendedor origen ===
    
    /**
     * Suma los ingresos generados por pagos de lote de un vendedor específico.
     */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.vendedorOrigen.id = :vendedorId 
        AND m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.INGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PAGO_LOTE
        """)
    BigDecimal sumarIngresosPorVendedor(@Param("vendedorId") Long vendedorId);

    /**
     * Suma TRABIX vendidos por un vendedor que generaron ingreso al fondo.
     */
    @Query("""
        SELECT COALESCE(SUM(m.cantidadTrabix), 0) FROM MovimientoFondo m 
        WHERE m.vendedorOrigen.id = :vendedorId 
        AND m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.INGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PAGO_LOTE
        """)
    Long sumarTrabixPorVendedor(@Param("vendedorId") Long vendedorId);

    // === Conteos por período ===
    
    long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    
    long countByTipoAndFechaBetween(TipoMovimientoFondo tipo, LocalDateTime desde, LocalDateTime hasta);
    
    /**
     * Cuenta premios en un período.
     */
    @Query("""
        SELECT COUNT(m) FROM MovimientoFondo m 
        WHERE m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.EGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PREMIO
        AND m.fecha BETWEEN :desde AND :hasta
        """)
    long contarPremiosPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Cuenta ingresos por pago de lote en un período.
     */
    @Query("""
        SELECT COUNT(m) FROM MovimientoFondo m 
        WHERE m.tipo = com.trabix.finance.entity.TipoMovimientoFondo.INGRESO 
        AND m.referenciaTipo = com.trabix.finance.entity.ReferenciaMovimiento.PAGO_LOTE
        AND m.fecha BETWEEN :desde AND :hasta
        """)
    long contarPagosLotePeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // === Últimos movimientos ===
    
    List<MovimientoFondo> findTop10ByOrderByFechaDesc();
    
    List<MovimientoFondo> findTop10ByTipoOrderByFechaDesc(TipoMovimientoFondo tipo);
    
    List<MovimientoFondo> findTop10ByReferenciaTipoOrderByFechaDesc(ReferenciaMovimiento referenciaTipo);

    // === Por referencia ===
    
    List<MovimientoFondo> findByReferenciaIdAndReferenciaTipo(Long referenciaId, ReferenciaMovimiento referenciaTipo);
    
    boolean existsByReferenciaIdAndReferenciaTipo(Long referenciaId, ReferenciaMovimiento referenciaTipo);

    // === Estadísticas ===
    
    @Query("""
        SELECT m.referenciaTipo, COUNT(m), COALESCE(SUM(m.monto), 0) 
        FROM MovimientoFondo m 
        WHERE m.tipo = :tipo
        GROUP BY m.referenciaTipo
        ORDER BY SUM(m.monto) DESC
        """)
    List<Object[]> obtenerResumenPorReferencia(@Param("tipo") TipoMovimientoFondo tipo);
}
