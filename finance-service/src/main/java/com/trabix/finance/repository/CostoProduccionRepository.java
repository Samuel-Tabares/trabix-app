package com.trabix.finance.repository;

import com.trabix.finance.entity.CostoProduccion;
import com.trabix.finance.entity.TipoCosto;
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

    // === Consultas paginadas ===
    
    Page<CostoProduccion> findByTipo(TipoCosto tipo, Pageable pageable);
    
    Page<CostoProduccion> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    Page<CostoProduccion> findByTipoAndFechaBetween(
            TipoCosto tipo, 
            LocalDateTime desde, 
            LocalDateTime hasta, 
            Pageable pageable
    );

    // === Búsquedas ===
    
    @Query("""
        SELECT c FROM CostoProduccion c 
        WHERE LOWER(c.concepto) LIKE LOWER(CONCAT('%', :concepto, '%'))
        ORDER BY c.fecha DESC
        """)
    List<CostoProduccion> buscarPorConcepto(@Param("concepto") String concepto);
    
    @Query("""
        SELECT c FROM CostoProduccion c 
        WHERE LOWER(c.proveedor) LIKE LOWER(CONCAT('%', :proveedor, '%'))
        ORDER BY c.fecha DESC
        """)
    List<CostoProduccion> buscarPorProveedor(@Param("proveedor") String proveedor);

    // === Sumas y agregaciones ===
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c")
    BigDecimal sumarTotalCostos();
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.tipo = :tipo")
    BigDecimal sumarCostosPorTipo(@Param("tipo") TipoCosto tipo);
    
    @Query("SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c WHERE c.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarCostosPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
    
    @Query("""
        SELECT COALESCE(SUM(c.costoTotal), 0) FROM CostoProduccion c 
        WHERE c.tipo = :tipo AND c.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarCostosPorTipoYPeriodo(
            @Param("tipo") TipoCosto tipo, 
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta
    );

    // === Conteos ===
    
    long countByTipo(TipoCosto tipo);
    
    long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    
    long countByTipoAndFechaBetween(TipoCosto tipo, LocalDateTime desde, LocalDateTime hasta);

    // === Resúmenes por tipo ===
    
    @Query("""
        SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) 
        FROM CostoProduccion c 
        GROUP BY c.tipo
        ORDER BY SUM(c.costoTotal) DESC
        """)
    List<Object[]> obtenerResumenPorTipo();
    
    @Query("""
        SELECT c.tipo, COUNT(c), COALESCE(SUM(c.costoTotal), 0) 
        FROM CostoProduccion c 
        WHERE c.fecha BETWEEN :desde AND :hasta 
        GROUP BY c.tipo
        ORDER BY SUM(c.costoTotal) DESC
        """)
    List<Object[]> obtenerResumenPorTipoPeriodo(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta
    );

    // === Últimos registros ===
    
    List<CostoProduccion> findTop10ByOrderByFechaDesc();
    
    List<CostoProduccion> findTop10ByTipoOrderByFechaDesc(TipoCosto tipo);

    // === Por proveedor ===
    
    List<CostoProduccion> findByProveedorIgnoreCaseOrderByFechaDesc(String proveedor);
    
    @Query("SELECT DISTINCT c.proveedor FROM CostoProduccion c WHERE c.proveedor IS NOT NULL ORDER BY c.proveedor")
    List<String> listarProveedores();

    // === Por factura ===
    
    boolean existsByNumeroFactura(String numeroFactura);
}
