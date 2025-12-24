package com.trabix.billing.repository;

import com.trabix.billing.entity.Cuadre;
import com.trabix.common.enums.TipoCuadre;
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
public interface CuadreRepository extends JpaRepository<Cuadre, Long> {

    // === Consultas por tanda ===
    
    List<Cuadre> findByTandaIdOrderByCreatedAtDesc(Long tandaId);
    
    Optional<Cuadre> findByTandaIdAndTipo(Long tandaId, TipoCuadre tipo);
    
    Optional<Cuadre> findFirstByTandaIdOrderByCreatedAtDesc(Long tandaId);
    
    // === Consultas por estado ===
    
    Page<Cuadre> findByEstado(String estado, Pageable pageable);
    
    List<Cuadre> findByEstadoOrderByCreatedAtAsc(String estado);
    
    long countByEstado(String estado);
    
    // === Consultas por lote (a través de tanda) ===
    
    @Query("""
        SELECT c FROM Cuadre c 
        JOIN c.tanda t 
        WHERE t.lote.id = :loteId 
        ORDER BY c.createdAt DESC
        """)
    List<Cuadre> findByLoteId(@Param("loteId") Long loteId);
    
    // === Consultas por usuario (a través de lote) ===
    
    @Query("""
        SELECT c FROM Cuadre c 
        JOIN c.tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        ORDER BY c.createdAt DESC
        """)
    List<Cuadre> findByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Query("""
        SELECT c FROM Cuadre c 
        JOIN c.tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND c.estado = :estado
        """)
    List<Cuadre> findByUsuarioIdAndEstado(
            @Param("usuarioId") Long usuarioId, 
            @Param("estado") String estado);
    
    // === Verificaciones ===
    
    /** Verifica si existe un cuadre pendiente para una tanda */
    boolean existsByTandaIdAndEstado(Long tandaId, String estado);
    
    /** Verifica si ya existe un cuadre de cierto tipo para una tanda */
    boolean existsByTandaIdAndTipo(Long tandaId, TipoCuadre tipo);
    
    // === Estadísticas ===
    
    /** Suma el excedente de todos los cuadres exitosos de un lote */
    @Query("""
        SELECT COALESCE(SUM(c.excedente), 0) FROM Cuadre c 
        JOIN c.tanda t 
        WHERE t.lote.id = :loteId 
        AND c.estado = 'EXITOSO'
        """)
    BigDecimal sumarExcedentePorLote(@Param("loteId") Long loteId);
    
    /** Obtiene el último excedente de un lote */
    @Query("""
        SELECT c.excedente FROM Cuadre c 
        JOIN c.tanda t 
        WHERE t.lote.id = :loteId 
        AND c.estado = 'EXITOSO' 
        ORDER BY c.fecha DESC 
        LIMIT 1
        """)
    Optional<BigDecimal> obtenerUltimoExcedente(@Param("loteId") Long loteId);
    
    /** Cuenta cuadres exitosos de un lote */
    @Query("""
        SELECT COUNT(c) FROM Cuadre c 
        JOIN c.tanda t 
        WHERE t.lote.id = :loteId 
        AND c.estado = 'EXITOSO'
        """)
    long contarCuadresExitososPorLote(@Param("loteId") Long loteId);
}
