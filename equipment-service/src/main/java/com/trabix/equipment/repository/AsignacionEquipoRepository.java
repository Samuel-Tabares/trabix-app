package com.trabix.equipment.repository;

import com.trabix.equipment.entity.AsignacionEquipo;
import com.trabix.equipment.entity.EstadoAsignacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionEquipoRepository extends JpaRepository<AsignacionEquipo, Long> {

    // === Por usuario ===
    
    List<AsignacionEquipo> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
    
    List<AsignacionEquipo> findByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    /**
     * Busca si el usuario tiene una asignación activa.
     * Solo puede tener 1 kit activo.
     */
    Optional<AsignacionEquipo> findFirstByUsuarioIdAndEstado(
            Long usuarioId,
            EstadoAsignacion estado
    );
    
    /**
     * Verifica si el usuario ya tiene un kit activo.
     */
    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoAsignacion estado);
    
    /**
     * Obtiene la asignación activa de un usuario (si existe).
     */
    @Query("SELECT a FROM AsignacionEquipo a WHERE a.usuario.id = :usuarioId AND a.estado = com.trabix.equipment.entity.EstadoAsignacion.ACTIVO")
    Optional<AsignacionEquipo> findAsignacionActivaByUsuario(@Param("usuarioId") Long usuarioId);

    // === Por estado ===
    
    Page<AsignacionEquipo> findByEstado(EstadoAsignacion estado, Pageable pageable);
    
    List<AsignacionEquipo> findByEstadoOrderByFechaInicioDesc(EstadoAsignacion estado);
    
    long countByEstado(EstadoAsignacion estado);

    // === Con bloqueo para operaciones críticas ===
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AsignacionEquipo a WHERE a.id = :id")
    Optional<AsignacionEquipo> findByIdForUpdate(@Param("id") Long id);

    // === Estadísticas ===
    
    @Query("SELECT COUNT(a) FROM AsignacionEquipo a WHERE a.usuario.id = :usuarioId AND a.estado = com.trabix.equipment.entity.EstadoAsignacion.ACTIVO")
    long countAsignacionesActivasByUsuario(@Param("usuarioId") Long usuarioId);
    
    /**
     * Cuenta asignaciones activas globales.
     */
    @Query("SELECT COUNT(a) FROM AsignacionEquipo a WHERE a.estado = com.trabix.equipment.entity.EstadoAsignacion.ACTIVO")
    long countAsignacionesActivas();

    // === Canceladas pendientes de reposición ===
    
    @Query("""
        SELECT a FROM AsignacionEquipo a 
        WHERE a.estado = com.trabix.equipment.entity.EstadoAsignacion.CANCELADO 
        AND a.reposicionPagada = false
        ORDER BY a.fechaFinalizacion DESC
        """)
    List<AsignacionEquipo> findCanceladasPendientesReposicion();
    
    @Query("""
        SELECT a FROM AsignacionEquipo a 
        WHERE a.usuario.id = :usuarioId 
        AND a.estado = com.trabix.equipment.entity.EstadoAsignacion.CANCELADO 
        AND a.reposicionPagada = false
        """)
    List<AsignacionEquipo> findCanceladasPendientesReposicionByUsuario(@Param("usuarioId") Long usuarioId);
}
