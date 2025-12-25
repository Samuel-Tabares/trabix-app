package com.trabix.equipment.repository;

import com.trabix.equipment.entity.Equipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
    
    List<Equipo> findByUsuarioIdAndEstado(Long usuarioId, String estado);
    
    Page<Equipo> findByEstado(String estado, Pageable pageable);
    
    Page<Equipo> findByTipo(String tipo, Pageable pageable);
    
    Page<Equipo> findByTipoAndEstado(String tipo, String estado, Pageable pageable);
    
    long countByUsuarioIdAndEstado(Long usuarioId, String estado);
    
    long countByEstado(String estado);
    
    long countByTipo(String tipo);
    
    @Query("SELECT e FROM Equipo e WHERE e.usuario.id = :usuarioId AND e.estado = 'ACTIVO'")
    List<Equipo> findEquiposActivosByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT COUNT(e) FROM Equipo e WHERE e.estado = 'ACTIVO'")
    long countEquiposActivos();
    
    @Query("SELECT e.tipo, COUNT(e) FROM Equipo e WHERE e.estado = 'ACTIVO' GROUP BY e.tipo")
    List<Object[]> contarEquiposActivosPorTipo();
    
    boolean existsByUsuarioIdAndTipoAndEstado(Long usuarioId, String tipo, String estado);
}
