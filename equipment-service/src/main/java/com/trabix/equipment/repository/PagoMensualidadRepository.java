package com.trabix.equipment.repository;

import com.trabix.equipment.entity.EstadoPago;
import com.trabix.equipment.entity.PagoMensualidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoMensualidadRepository extends JpaRepository<PagoMensualidad, Long> {

    // === Por asignación ===
    
    List<PagoMensualidad> findByAsignacionIdOrderByAnioDescMesDesc(Long asignacionId);
    
    Optional<PagoMensualidad> findByAsignacionIdAndMesAndAnio(Long asignacionId, Integer mes, Integer anio);
    
    boolean existsByAsignacionIdAndMesAndAnio(Long asignacionId, Integer mes, Integer anio);
    
    // === Por estado ===
    
    Page<PagoMensualidad> findByEstado(EstadoPago estado, Pageable pageable);
    
    List<PagoMensualidad> findByEstadoOrderByAnioAscMesAsc(EstadoPago estado);
    
    long countByEstado(EstadoPago estado);

    // === Pagos pendientes por asignación ===
    
    @Query("""
        SELECT p FROM PagoMensualidad p 
        WHERE p.asignacion.id = :asignacionId 
        AND (p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
             OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO)
        ORDER BY p.anio ASC, p.mes ASC
        """)
    List<PagoMensualidad> findPagosPendientesByAsignacion(@Param("asignacionId") Long asignacionId);
    
    @Query("""
        SELECT COUNT(p) FROM PagoMensualidad p 
        WHERE p.asignacion.id = :asignacionId 
        AND (p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
             OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO)
        """)
    long countPagosPendientesByAsignacion(@Param("asignacionId") Long asignacionId);

    // === Pagos pendientes por usuario ===
    
    @Query("""
        SELECT p FROM PagoMensualidad p 
        WHERE p.asignacion.usuario.id = :usuarioId 
        AND (p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
             OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO)
        ORDER BY p.anio ASC, p.mes ASC
        """)
    List<PagoMensualidad> findPagosPendientesByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("""
        SELECT COUNT(p) FROM PagoMensualidad p 
        WHERE p.asignacion.usuario.id = :usuarioId 
        AND (p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
             OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO)
        """)
    long countPagosPendientesByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("""
        SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p 
        WHERE p.asignacion.usuario.id = :usuarioId 
        AND (p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
             OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO)
        """)
    BigDecimal sumarMontoPendienteByUsuario(@Param("usuarioId") Long usuarioId);

    // === Totales globales ===
    
    @Query("""
        SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p 
        WHERE p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
           OR p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO
        """)
    BigDecimal sumarTotalPendiente();
    
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p WHERE p.estado = com.trabix.equipment.entity.EstadoPago.PAGADO")
    BigDecimal sumarTotalPagado();

    // === Por mes y año ===
    
    @Query("SELECT p FROM PagoMensualidad p WHERE p.mes = :mes AND p.anio = :anio ORDER BY p.asignacion.usuario.nombre")
    List<PagoMensualidad> findByMesYAnio(@Param("mes") Integer mes, @Param("anio") Integer anio);
    
    @Query("""
        SELECT COUNT(p) FROM PagoMensualidad p 
        WHERE p.mes = :mes AND p.anio = :anio AND p.estado = :estado
        """)
    long countByMesAnioYEstado(@Param("mes") Integer mes, @Param("anio") Integer anio, @Param("estado") EstadoPago estado);

    // === Marcar vencidos automáticamente ===
    
    @Modifying
    @Query("""
        UPDATE PagoMensualidad p 
        SET p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO,
            p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.estado = com.trabix.equipment.entity.EstadoPago.PENDIENTE 
        AND p.fechaVencimiento < :hoy
        """)
    int marcarPagosVencidos(@Param("hoy") LocalDate hoy);

    // === Pagos vencidos ===
    
    @Query("""
        SELECT p FROM PagoMensualidad p 
        WHERE p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO
        ORDER BY p.fechaVencimiento ASC
        """)
    List<PagoMensualidad> findPagosVencidos();
    
    @Query("""
        SELECT p FROM PagoMensualidad p 
        WHERE p.asignacion.usuario.id = :usuarioId 
        AND p.estado = com.trabix.equipment.entity.EstadoPago.VENCIDO
        ORDER BY p.fechaVencimiento ASC
        """)
    List<PagoMensualidad> findPagosVencidosByUsuario(@Param("usuarioId") Long usuarioId);
}
