package com.trabix.finance.repository;

import com.trabix.finance.entity.CostoProduccion;
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
public interface CostoProduccionRepository extends JpaRepository<CostoProduccion, Long> {

    Page<CostoProduccion> findByTipo(String tipo, Pageable pageable);
    
    Page<CostoProduccion> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    @Query("""
        SELECT c FROM CostoProduccion c 
        WHERE LOWER(c.concepto) LIKE LOWER(CONCAT('%', :concepto, '%'))
        ORDER BY c.fecha DESC
        """)
    List<CostoProduccion> buscarPorConcepto(@Param("concepto") String concepto);
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c")
    BigDecimal sumarTotalCostos();
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.tipo = :tipo")
    BigDecimal sumarCostosPorTipo(@Param("tipo") String tipo);
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarCostosPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.tipo = :tipo AND c.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarCostosPorTipoYPeriodo(@Param("tipo") String tipo, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    long countByTipo(String tipo);
    
    long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    
    @Query("SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c GROUP BY c.tipo")
    List<Object[]> obtenerResumenPorTipo();
    
    @Query("SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.fecha BETWEEN :desde AND :hasta GROUP BY c.tipo")
    List<Object[]> obtenerResumenPorTipoPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    List<CostoProduccion> findTop10ByOrderByFechaDesc();
}
