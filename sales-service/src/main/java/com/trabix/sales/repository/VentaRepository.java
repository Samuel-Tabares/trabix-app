package com.trabix.sales.repository;

import com.trabix.common.enums.EstadoVenta;
import com.trabix.common.enums.TipoVenta;
import com.trabix.sales.entity.Venta;
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
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // === Consultas por usuario ===
    
    List<Venta> findByUsuarioIdOrderByFechaRegistroDesc(Long usuarioId);

    Page<Venta> findByUsuarioId(Long usuarioId, Pageable pageable);

    List<Venta> findByUsuarioIdAndEstado(Long usuarioId, EstadoVenta estado);

    // === Consultas por tanda ===
    
    List<Venta> findByTandaIdOrderByFechaRegistroDesc(Long tandaId);

    List<Venta> findByTandaIdAndEstado(Long tandaId, EstadoVenta estado);

    // === Consultas por estado ===
    
    Page<Venta> findByEstado(EstadoVenta estado, Pageable pageable);

    long countByEstado(EstadoVenta estado);

    // === Consultas por tipo ===
    
    List<Venta> findByUsuarioIdAndTipo(Long usuarioId, TipoVenta tipo);

    // === Consultas de rango de fechas ===
    
    List<Venta> findByUsuarioIdAndFechaRegistroBetween(
            Long usuarioId, LocalDateTime desde, LocalDateTime hasta);

    Page<Venta> findByFechaRegistroBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    // === Estadísticas ===
    
    // Contar ventas por usuario y estado
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoVenta estado);

    // Contar unidades por usuario, tipo y estado
    @Query("""
        SELECT COALESCE(SUM(v.cantidad), 0) FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.tipo = :tipo 
        AND v.estado = :estado
        """)
    int sumarUnidadesPorTipoYEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoVenta tipo,
            @Param("estado") EstadoVenta estado);

    // Sumar total recaudado por usuario y estado
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.estado = :estado 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorUsuarioYEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoVenta estado);

    // Sumar total recaudado por tanda y estado
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = :estado 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorTandaYEstado(
            @Param("tandaId") Long tandaId,
            @Param("estado") EstadoVenta estado);

    // Contar regalos por tanda
    @Query("""
        SELECT COALESCE(SUM(v.cantidad), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.tipo = 'REGALO' 
        AND v.estado != 'RECHAZADA'
        """)
    int contarRegalosPorTanda(@Param("tandaId") Long tandaId);

    // Estadísticas completas de un usuario
    @Query("""
        SELECT 
            COUNT(v), 
            COALESCE(SUM(v.cantidad), 0),
            COALESCE(SUM(CASE WHEN v.tipo != 'REGALO' THEN v.precioTotal ELSE 0 END), 0)
        FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.estado = 'APROBADA'
        """)
    Object[] obtenerEstadisticasUsuario(@Param("usuarioId") Long usuarioId);

    // Estadísticas por tipo para un usuario
    @Query("""
        SELECT v.tipo, COUNT(v), COALESCE(SUM(v.cantidad), 0), COALESCE(SUM(v.precioTotal), 0)
        FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.estado = 'APROBADA'
        GROUP BY v.tipo
        """)
    List<Object[]> obtenerEstadisticasPorTipo(@Param("usuarioId") Long usuarioId);

    // Sumar ganancias del vendedor
    @Query("""
        SELECT COALESCE(SUM(v.gananciaVendedor), 0) FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.estado = 'APROBADA'
        """)
    BigDecimal sumarGananciaVendedor(@Param("usuarioId") Long usuarioId);

    // Sumar parte de Samuel
    @Query("""
        SELECT COALESCE(SUM(v.parteSamuel), 0) FROM Venta v 
        WHERE v.usuario.id = :usuarioId 
        AND v.estado = 'APROBADA'
        """)
    BigDecimal sumarParteSamuel(@Param("usuarioId") Long usuarioId);

    // Sumar ganancias por tanda
    @Query("""
        SELECT COALESCE(SUM(v.gananciaVendedor), 0), COALESCE(SUM(v.parteSamuel), 0)
        FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = 'APROBADA'
        """)
    Object[] sumarGananciasPorTanda(@Param("tandaId") Long tandaId);
}
