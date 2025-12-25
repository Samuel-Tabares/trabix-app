package com.trabix.equipment.repository;

import com.trabix.equipment.entity.PagoMensualidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoMensualidadRepository extends JpaRepository<PagoMensualidad, Long> {

    List<PagoMensualidad> findByEquipoIdOrderByAnioDescMesDesc(Long equipoId);
    
    Page<PagoMensualidad> findByEstado(String estado, Pageable pageable);
    
    Optional<PagoMensualidad> findByEquipoIdAndMesAndAnio(Long equipoId, Integer mes, Integer anio);
    
    boolean existsByEquipoIdAndMesAndAnio(Long equipoId, Integer mes, Integer anio);
    
    // Pagos pendientes de un equipo
    List<PagoMensualidad> findByEquipoIdAndEstadoOrderByAnioAscMesAsc(Long equipoId, String estado);
    
    // Pagos pendientes de un usuario (a través de sus equipos)
    @Query("""
        SELECT p FROM PagoMensualidad p 
        WHERE p.equipo.usuario.id = :usuarioId AND p.estado = 'PENDIENTE'
        ORDER BY p.anio ASC, p.mes ASC
        """)
    List<PagoMensualidad> findPagosPendientesByUsuario(@Param("usuarioId") Long usuarioId);
    
    // Contar pendientes por usuario
    @Query("""
        SELECT COUNT(p) FROM PagoMensualidad p 
        WHERE p.equipo.usuario.id = :usuarioId AND p.estado = 'PENDIENTE'
        """)
    long countPagosPendientesByUsuario(@Param("usuarioId") Long usuarioId);
    
    // Total pendiente por usuario
    @Query("""
        SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p 
        WHERE p.equipo.usuario.id = :usuarioId AND p.estado = 'PENDIENTE'
        """)
    BigDecimal sumarMontoPendienteByUsuario(@Param("usuarioId") Long usuarioId);
    
    // Total pendiente global
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p WHERE p.estado = 'PENDIENTE'")
    BigDecimal sumarTotalPendiente();
    
    // Total pagado global
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoMensualidad p WHERE p.estado = 'PAGADO'")
    BigDecimal sumarTotalPagado();
    
    // Contar por estado
    long countByEstado(String estado);
    
    // Pagos de un mes específico
    @Query("SELECT p FROM PagoMensualidad p WHERE p.mes = :mes AND p.anio = :anio ORDER BY p.equipo.usuario.nombre")
    List<PagoMensualidad> findByMesYAnio(@Param("mes") Integer mes, @Param("anio") Integer anio);
    
    // Pagos pendientes globales ordenados
    @Query("SELECT p FROM PagoMensualidad p WHERE p.estado = 'PENDIENTE' ORDER BY p.anio ASC, p.mes ASC")
    List<PagoMensualidad> findAllPendientesOrdenados();
}
