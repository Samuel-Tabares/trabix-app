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

    // === Consultas básicas ===
    
    List<CostoProduccion> findByTipoOrderByFechaDesc(String tipo);
    
    Page<CostoProduccion> findByTipo(String tipo, Pageable pageable);
    
    // === Consultas por rango de fechas ===
    
    List<CostoProduccion> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime desde, LocalDateTime hasta);
    
    Page<CostoProduccion> findByFechaBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    // === Consultas por tipo y fecha ===
    
    List<CostoProduccion> findByTipoAndFechaBetweenOrderByFechaDesc(
            String tipo, LocalDateTime desde, LocalDateTime hasta);
    
    // === Búsqueda por concepto ===
    
    @Query("""
        SELECT c FROM CostoProduccion c 
        WHERE LOWER(c.concepto) LIKE LOWER(CONCAT('%', :concepto, '%'))
        ORDER BY c.fecha DESC
        """)
    List<CostoProduccion> buscarPorConcepto(@Param("concepto") String concepto);
    
    // === Estadísticas ===
    
    /** Suma total de costos */
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c")
    BigDecimal sumarTotalCostos();
    
    /** Suma costos por tipo */
    @Query("""
        SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c 
        WHERE c.tipo = :tipo
        """)
    BigDecimal sumarCostosPorTipo(@Param("tipo") String tipo);
    
    /** Suma costos en un período */
    @Query("""
        SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c 
        WHERE c.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarCostosPeriodo(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta);
    
    /** Suma costos por tipo en un período */
    @Query("""
        SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c 
        WHERE c.tipo = :tipo 
        AND c.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarCostosPorTipoYPeriodo(
            @Param("tipo") String tipo,
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta);
    
    /** Cuenta registros por tipo */
    long countByTipo(String tipo);
    
    /** Obtiene resumen por tipo (tipo, count, total) */
    @Query("""
        SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) 
        FROM CostoProduccion c 
        GROUP BY c.tipo
        """)
    List<Object[]> obtenerResumenPorTipo();
    
    /** Obtiene resumen por tipo en un período */
    @Query("""
        SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) 
        FROM CostoProduccion c 
        WHERE c.fecha BETWEEN :desde AND :hasta
        GROUP BY c.tipo
        """)
    List<Object[]> obtenerResumenPorTipoPeriodo(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta);
    
    /** Obtiene los últimos N registros */
    List<CostoProduccion> findTop10ByOrderByFechaDesc();
    
    /** Costos por proveedor */
    List<CostoProduccion> findByProveedorOrderByFechaDesc(String proveedor);
    
    /** Suma costos por proveedor */
    @Query("""
        SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c 
        WHERE c.proveedor = :proveedor
        """)
    BigDecimal sumarCostosPorProveedor(@Param("proveedor") String proveedor);
}
