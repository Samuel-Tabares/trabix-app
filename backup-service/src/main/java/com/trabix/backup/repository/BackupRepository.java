package com.trabix.backup.repository;

import com.trabix.backup.entity.Backup;
import com.trabix.backup.entity.EstadoBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {

    Page<Backup> findByEstado(EstadoBackup estado, Pageable pageable);
    
    List<Backup> findByEstadoOrderByFechaInicioDesc(EstadoBackup estado);
    
    List<Backup> findTop10ByOrderByFechaInicioDesc();
    
    Optional<Backup> findTopByEstadoOrderByFechaInicioDesc(EstadoBackup estado);
    
    Optional<Backup> findTopByOrderByFechaInicioDesc();
    
    long countByEstado(EstadoBackup estado);
    
    /**
     * Verifica si hay un backup en proceso.
     */
    boolean existsByEstado(EstadoBackup estado);
    
    @Query("SELECT COALESCE(SUM(b.tamanoBytes), 0) FROM Backup b WHERE b.estado = com.trabix.backup.entity.EstadoBackup.COMPLETADO")
    Long sumarTamanoTotal();
    
    @Query("SELECT b FROM Backup b WHERE b.estado = com.trabix.backup.entity.EstadoBackup.COMPLETADO ORDER BY b.fechaInicio DESC")
    List<Backup> findCompletadosOrdenados();
}
