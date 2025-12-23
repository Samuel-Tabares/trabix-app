package com.trabix.sales.repository;

import com.trabix.sales.entity.Tanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TandaRepository extends JpaRepository<Tanda, Long> {

    /**
     * Busca la tanda activa (liberada con stock) de un usuario.
     */
    @Query(value = """
        SELECT t.* FROM tandas t
        JOIN lotes l ON t.lote_id = l.id
        WHERE l.usuario_id = :usuarioId
        AND l.estado = 'ACTIVO'
        AND t.estado = 'LIBERADA'
        AND t.stock_actual > 0
        ORDER BY t.numero ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Tanda> findTandaActivaDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Sumar stock actual de un usuario.
     */
    @Query(value = """
        SELECT COALESCE(SUM(t.stock_actual), 0) FROM tandas t
        JOIN lotes l ON t.lote_id = l.id
        WHERE l.usuario_id = :usuarioId
        AND l.estado = 'ACTIVO'
        AND t.estado = 'LIBERADA'
        """, nativeQuery = true)
    int sumarStockActualUsuario(@Param("usuarioId") Long usuarioId);
}
