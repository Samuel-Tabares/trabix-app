package com.trabix.document.repository;

import com.trabix.document.entity.Documento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByNumero(String numero);
    
    // === Por tipo ===
    Page<Documento> findByTipo(String tipo, Pageable pageable);
    
    Page<Documento> findByTipoAndEstado(String tipo, String estado, Pageable pageable);
    
    List<Documento> findByTipoOrderByFechaEmisionDesc(String tipo);
    
    // === Por usuario ===
    Page<Documento> findByUsuarioId(Long usuarioId, Pageable pageable);
    
    Page<Documento> findByUsuarioIdAndTipo(Long usuarioId, String tipo, Pageable pageable);
    
    List<Documento> findByUsuarioIdAndTipoOrderByFechaEmisionDesc(Long usuarioId, String tipo);
    
    // === Por estado ===
    Page<Documento> findByEstado(String estado, Pageable pageable);
    
    List<Documento> findByTipoAndEstadoOrderByFechaEmisionDesc(String tipo, String estado);
    
    // === Por fechas ===
    Page<Documento> findByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    Page<Documento> findByTipoAndFechaEmisionBetween(String tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    // === Búsqueda por cliente ===
    @Query("""
        SELECT d FROM Documento d 
        WHERE LOWER(d.clienteNombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
        ORDER BY d.fechaEmision DESC
        """)
    List<Documento> buscarPorCliente(@Param("nombre") String nombre);
    
    @Query("""
        SELECT d FROM Documento d 
        WHERE d.tipo = :tipo 
        AND LOWER(d.clienteNombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
        ORDER BY d.fechaEmision DESC
        """)
    List<Documento> buscarPorClienteYTipo(@Param("nombre") String nombre, @Param("tipo") String tipo);
    
    // === Cotizaciones vencidas ===
    @Query("""
        SELECT d FROM Documento d 
        WHERE d.tipo = 'COTIZACION' 
        AND d.estado = 'EMITIDO'
        AND d.fechaVencimiento < :ahora
        """)
    List<Documento> findCotizacionesVencidas(@Param("ahora") LocalDateTime ahora);
    
    // === Estadísticas ===
    long countByTipo(String tipo);
    
    long countByTipoAndEstado(String tipo, String estado);
    
    @Query("SELECT COALESCE(SUM(d.total), 0) FROM Documento d WHERE d.tipo = :tipo AND d.estado = 'PAGADO'")
    BigDecimal sumarTotalPagadoPorTipo(@Param("tipo") String tipo);
    
    @Query("SELECT COALESCE(SUM(d.total), 0) FROM Documento d WHERE d.tipo = :tipo AND d.estado = 'EMITIDO'")
    BigDecimal sumarTotalPendientePorTipo(@Param("tipo") String tipo);
    
    @Query("""
        SELECT COALESCE(SUM(d.total), 0) FROM Documento d 
        WHERE d.tipo = :tipo 
        AND d.estado = 'PAGADO'
        AND d.fechaEmision BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarTotalPagadoPorTipoPeriodo(
            @Param("tipo") String tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
    
    // === Numeración ===
    @Query("""
        SELECT MAX(d.id) FROM Documento d 
        WHERE d.tipo = :tipo 
        AND YEAR(d.fechaEmision) = :anio
        """)
    Optional<Long> findMaxIdByTipoYAnio(@Param("tipo") String tipo, @Param("anio") int anio);
    
    // === Recientes ===
    List<Documento> findTop10ByTipoOrderByFechaEmisionDesc(String tipo);
    
    List<Documento> findTop10ByOrderByFechaEmisionDesc();
}
