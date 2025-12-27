package com.trabix.document.repository;

import com.trabix.document.entity.Documento;
import com.trabix.document.entity.EstadoDocumento;
import com.trabix.document.entity.TipoDocumento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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
    
    Page<Documento> findByTipo(TipoDocumento tipo, Pageable pageable);
    
    Page<Documento> findByTipoAndEstado(TipoDocumento tipo, EstadoDocumento estado, Pageable pageable);
    
    List<Documento> findByTipoOrderByFechaEmisionDesc(TipoDocumento tipo);
    
    // === Por usuario ===
    
    Page<Documento> findByUsuarioId(Long usuarioId, Pageable pageable);
    
    Page<Documento> findByUsuarioIdAndTipo(Long usuarioId, TipoDocumento tipo, Pageable pageable);
    
    // === Por estado ===
    
    Page<Documento> findByEstado(EstadoDocumento estado, Pageable pageable);
    
    List<Documento> findByTipoAndEstadoOrderByFechaEmisionDesc(TipoDocumento tipo, EstadoDocumento estado);
    
    // === Por fechas ===
    
    Page<Documento> findByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    
    Page<Documento> findByTipoAndFechaEmisionBetween(
            TipoDocumento tipo, 
            LocalDateTime desde, 
            LocalDateTime hasta, 
            Pageable pageable
    );
    
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
    List<Documento> buscarPorClienteYTipo(@Param("nombre") String nombre, @Param("tipo") TipoDocumento tipo);
    
    // === Cotizaciones vencidas ===
    
    @Query("""
        SELECT d FROM Documento d 
        WHERE d.tipo = com.trabix.document.entity.TipoDocumento.COTIZACION 
        AND d.estado = com.trabix.document.entity.EstadoDocumento.EMITIDO
        AND d.fechaVencimiento < :ahora
        """)
    List<Documento> findCotizacionesVencidas(@Param("ahora") LocalDateTime ahora);
    
    /**
     * Marca cotizaciones vencidas automáticamente.
     */
    @Modifying
    @Query("""
        UPDATE Documento d 
        SET d.estado = com.trabix.document.entity.EstadoDocumento.VENCIDO,
            d.updatedAt = :ahora
        WHERE d.tipo = com.trabix.document.entity.TipoDocumento.COTIZACION 
        AND d.estado = com.trabix.document.entity.EstadoDocumento.EMITIDO
        AND d.fechaVencimiento < :ahora
        """)
    int marcarCotizacionesVencidas(@Param("ahora") LocalDateTime ahora);
    
    // === Estadísticas ===
    
    long countByTipo(TipoDocumento tipo);
    
    long countByTipoAndEstado(TipoDocumento tipo, EstadoDocumento estado);
    
    @Query("""
        SELECT COALESCE(SUM(d.total), 0) FROM Documento d 
        WHERE d.tipo = :tipo AND d.estado = com.trabix.document.entity.EstadoDocumento.PAGADO
        """)
    BigDecimal sumarTotalPagadoPorTipo(@Param("tipo") TipoDocumento tipo);
    
    @Query("""
        SELECT COALESCE(SUM(d.total), 0) FROM Documento d 
        WHERE d.tipo = :tipo AND d.estado = com.trabix.document.entity.EstadoDocumento.EMITIDO
        """)
    BigDecimal sumarTotalPendientePorTipo(@Param("tipo") TipoDocumento tipo);
    
    @Query("""
        SELECT COALESCE(SUM(d.total), 0) FROM Documento d 
        WHERE d.tipo = :tipo 
        AND d.estado = com.trabix.document.entity.EstadoDocumento.PAGADO
        AND d.fechaEmision BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarTotalPagadoPorTipoPeriodo(
            @Param("tipo") TipoDocumento tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
    
    // === Numeración (con bloqueo para evitar duplicados) ===
    
    /**
     * Obtiene el último número consecutivo para un tipo y año.
     * Usa bloqueo pesimista para evitar duplicados en concurrencia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT MAX(CAST(SUBSTRING(d.numero, LENGTH(d.numero) - 4) AS int)) 
        FROM Documento d 
        WHERE d.tipo = :tipo 
        AND d.numero LIKE CONCAT(:prefijo, '-', :anio, '-%')
        """)
    Optional<Integer> findMaxConsecutivoByTipoYAnio(
            @Param("tipo") TipoDocumento tipo,
            @Param("prefijo") String prefijo,
            @Param("anio") int anio
    );
    
    // === Recientes ===
    
    List<Documento> findTop10ByTipoOrderByFechaEmisionDesc(TipoDocumento tipo);
    
    List<Documento> findTop10ByOrderByFechaEmisionDesc();
    
    // === Por cotización origen ===
    
    List<Documento> findByCotizacionOrigenId(Long cotizacionOrigenId);
    
    boolean existsByCotizacionOrigenId(Long cotizacionOrigenId);
}
