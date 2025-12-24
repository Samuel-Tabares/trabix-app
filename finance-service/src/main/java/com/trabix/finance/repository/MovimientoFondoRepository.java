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

    Page<MovimientoFondo> findByFondoId(Long fondoId, Pageable pageable);
    
    List<MovimientoFondo> findByBeneficiarioIdOrderByFechaDesc(Long beneficiarioId);
    
    List<MovimientoFondo> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = 'INGRESO'")
    BigDecimal sumarTotalIngresos();
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = 'EGRESO'")
    BigDecimal sumarTotalEgresos();
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = 'INGRESO' AND m.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarIngresosPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = 'EGRESO' AND m.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarEgresosPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoFondo m WHERE m.tipo = 'EGRESO' AND m.beneficiario.id = :beneficiarioId")
    BigDecimal sumarPremiosPorBeneficiario(@Param("beneficiarioId") Long beneficiarioId);
    
    long countByBeneficiarioId(Long beneficiarioId);
    
    List<MovimientoFondo> findTop10ByOrderByFechaDesc();
}
