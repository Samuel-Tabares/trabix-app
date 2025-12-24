package com.trabix.billing.repository;

import com.trabix.billing.entity.Tanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TandaRepository extends JpaRepository<Tanda, Long> {

    List<Tanda> findByLoteIdOrderByNumeroAsc(Long loteId);
    
    Optional<Tanda> findByLoteIdAndNumero(Long loteId, Integer numero);
    
    /** Tandas que requieren cuadre (liberadas con stock <= porcentaje) */
    @Query("""
        SELECT t FROM Tanda t 
        WHERE t.estado = 'LIBERADA' 
        AND t.stockActual <= (t.stockEntregado * :porcentaje / 100.0)
        AND t.stockEntregado > 0
        """)
    List<Tanda> findTandasParaCuadre(@Param("porcentaje") int porcentaje);
    
    /** Tandas liberadas de un usuario */
    @Query("""
        SELECT t FROM Tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA'
        ORDER BY t.numero ASC
        """)
    List<Tanda> findTandasActivasDeUsuario(@Param("usuarioId") Long usuarioId);
    
    /** Verifica si la tanda anterior está cuadrada */
    @Query("""
        SELECT CASE WHEN t.estado = 'CUADRADA' THEN true ELSE false END 
        FROM Tanda t 
        WHERE t.lote.id = :loteId AND t.numero = :numero
        """)
    boolean isTandaCuadrada(@Param("loteId") Long loteId, @Param("numero") int numero);
}
