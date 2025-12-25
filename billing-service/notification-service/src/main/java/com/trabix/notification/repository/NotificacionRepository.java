package com.trabix.notification.repository;

import com.trabix.notification.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // === Por usuario ===
    
    @Query("""
        SELECT n FROM Notificacion n 
        WHERE n.usuario.id = :usuarioId OR n.usuario IS NULL
        ORDER BY n.createdAt DESC
        """)
    Page<Notificacion> findByUsuarioIdOrBroadcast(@Param("usuarioId") Long usuarioId, Pageable pageable);
    
    @Query("""
        SELECT n FROM Notificacion n 
        WHERE (n.usuario.id = :usuarioId OR n.usuario IS NULL) AND n.leida = false
        ORDER BY n.createdAt DESC
        """)
    List<Notificacion> findNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("""
        SELECT COUNT(n) FROM Notificacion n 
        WHERE (n.usuario.id = :usuarioId OR n.usuario IS NULL) AND n.leida = false
        """)
    long countNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);
    
    // === Por tipo ===
    
    Page<Notificacion> findByTipo(String tipo, Pageable pageable);
    
    @Query("""
        SELECT n FROM Notificacion n 
        WHERE (n.usuario.id = :usuarioId OR n.usuario IS NULL) AND n.tipo = :tipo
        ORDER BY n.createdAt DESC
        """)
    List<Notificacion> findByUsuarioIdAndTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo);
    
    // === Broadcast ===
    
    @Query("SELECT n FROM Notificacion n WHERE n.usuario IS NULL ORDER BY n.createdAt DESC")
    Page<Notificacion> findBroadcasts(Pageable pageable);
    
    // === Marcar como leídas ===
    
    @Modifying
    @Query("""
        UPDATE Notificacion n 
        SET n.leida = true, n.fechaLectura = :ahora 
        WHERE (n.usuario.id = :usuarioId OR n.usuario IS NULL) AND n.leida = false
        """)
    int marcarTodasLeidasByUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
    
    @Modifying
    @Query("""
        UPDATE Notificacion n 
        SET n.leida = true, n.fechaLectura = :ahora 
        WHERE n.id IN :ids AND n.leida = false
        """)
    int marcarLeidasByIds(@Param("ids") List<Long> ids, @Param("ahora") LocalDateTime ahora);
    
    // === Eliminación ===
    
    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.usuario.id = :usuarioId AND n.leida = true")
    int eliminarLeidasByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.createdAt < :fecha")
    int eliminarAntiguasMayorA(@Param("fecha") LocalDateTime fecha);
    
    // === Recientes ===
    
    List<Notificacion> findTop10ByOrderByCreatedAtDesc();
    
    @Query("""
        SELECT n FROM Notificacion n 
        WHERE n.usuario.id = :usuarioId OR n.usuario IS NULL
        ORDER BY n.createdAt DESC
        LIMIT 10
        """)
    List<Notificacion> findTop10ByUsuario(@Param("usuarioId") Long usuarioId);
    
    // === Por referencia ===
    
    List<Notificacion> findByReferenciaTipoAndReferenciaId(String referenciaTipo, Long referenciaId);
    
    // === Estadísticas ===
    
    long countByLeida(boolean leida);
    
    long countByTipo(String tipo);
    
    @Query("SELECT n.tipo, COUNT(n) FROM Notificacion n GROUP BY n.tipo")
    List<Object[]> contarPorTipo();
}
