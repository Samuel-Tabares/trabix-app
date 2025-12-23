package com.trabix.inventory.repository;

import com.trabix.common.enums.EstadoTanda;
import com.trabix.inventory.entity.Tanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TandaRepository extends JpaRepository<Tanda, Long> {

    // Tandas de un lote
    List<Tanda> findByLoteIdOrderByNumeroAsc(Long loteId);

    // Tanda específica de un lote
    Optional<Tanda> findByLoteIdAndNumero(Long loteId, Integer numero);

    // Tandas por estado
    List<Tanda> findByEstado(EstadoTanda estado);

    // Tandas liberadas de un lote
    List<Tanda> findByLoteIdAndEstado(Long loteId, EstadoTanda estado);

    // Tanda activa (liberada) de un usuario
    @Query("""
        SELECT t FROM Tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA'
        ORDER BY t.numero ASC
        """)
    List<Tanda> findTandasActivasDeUsuario(@Param("usuarioId") Long usuarioId);

    // Tanda actual para vender (primera liberada con stock > 0)
    @Query("""
        SELECT t FROM Tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA' 
        AND t.stockActual > 0
        ORDER BY t.numero ASC
        """)
    Optional<Tanda> findTandaActualParaVenta(@Param("usuarioId") Long usuarioId);

    // Tandas que requieren cuadre (stock <= 20%)
    @Query("""
        SELECT t FROM Tanda t 
        WHERE t.estado = 'LIBERADA' 
        AND t.stockActual <= (t.stockEntregado * :porcentaje / 100.0)
        """)
    List<Tanda> findTandasParaCuadre(@Param("porcentaje") int porcentaje);

    // Sumar stock actual de un usuario (todas sus tandas liberadas)
    @Query("""
        SELECT COALESCE(SUM(t.stockActual), 0) FROM Tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA'
        """)
    int sumarStockActualUsuario(@Param("usuarioId") Long usuarioId);
}
