package com.trabix.finance.repository;

import com.trabix.finance.entity.MovimientoFondo;
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

    // === Consultas básicas ===
    
    List<MovimientoFondo> findByFondoIdOrderByFechaDesc(Long fondoId);
    
    Page<MovimientoFondo> findByFondoId(Long fondoId, Pageable pageable);
    
    // === Consultas por tipo ===
    
    List<MovimientoFondo> findByTipoOrderByFechaDesc(String tipo);
    
    Page<MovimientoFondo> findByTipo(String tipo, Pageable pageable);
    
    // === Consultas por beneficiario ===
    
    List<MovimientoFondo> findByBeneficiarioIdOrderByFechaDesc(Long beneficiarioId);
    
    // === Consultas por rango de fechas ===
    
    List<MovimientoFondo> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime desde, LocalDateTime hasta);
    
    Page<MovimientoFondo> findByFechaBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    // === Estadísticas ===
    
    /** Suma total de ingresos */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = 'INGRESO'
        """)
    BigDecimal sumarTotalIngresos();
    
    /** Suma total de egresos */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = 'EGRESO'
        """)
    BigDecimal sumarTotalEgresos();
    
    /** Suma ingresos en un período */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = 'INGRESO' 
        AND m.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarIngresosPeriodo(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta);
    
    /** Suma egresos en un período */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = 'EGRESO' 
        AND m.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarEgresosPeriodo(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta);
    
    /** Cuenta movimientos por tipo */
    long countByTipo(String tipo);
    
    /** Suma premios entregados a un beneficiario */
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m 
        WHERE m.tipo = 'EGRESO' 
        AND m.beneficiario.id = :beneficiarioId
        """)
    BigDecimal sumarPremiosPorBeneficiario(@Param("beneficiarioId") Long beneficiarioId);
    
    /** Obtiene los últimos N movimientos */
    List<MovimientoFondo> findTop10ByOrderByFechaDesc();
    
    /** Movimientos por referencia (ej: cuadre específico) */
    List<MovimientoFondo> findByReferenciaTipoAndReferenciaId(String tipo, Long referenciaId);
}
