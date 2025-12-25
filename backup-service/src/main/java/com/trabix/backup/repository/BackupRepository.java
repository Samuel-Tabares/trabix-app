package com.trabix.backup.repository;

import com.trabix.backup.entity.Backup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {

    Page<Backup> findByEstado(String estado, Pageable pageable);
    
    List<Backup> findByEstadoOrderByFechaInicioDesc(String estado);
    
    List<Backup> findTop10ByOrderByFechaInicioDesc();
    
    Optional<Backup> findTopByOrderByFechaInicioDesc();
    
    long countByEstado(String estado);
    
    @Query("SELECT COALESCE(SUM(b.tamanoBytes), 0) FROM Backup b WHERE b.estado = 'COMPLETADO'")
    Long sumarTamanoTotal();
    
    @Query("SELECT b FROM Backup b WHERE b.estado = 'COMPLETADO' ORDER BY b.fechaInicio DESC")
    List<Backup> findCompletadosOrdenados();
    
    boolean existsByNombre(String nombre);
}
