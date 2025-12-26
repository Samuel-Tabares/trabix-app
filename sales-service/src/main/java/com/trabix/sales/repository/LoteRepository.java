package com.trabix.sales.repository;

import com.trabix.sales.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    /**
     * Busca lotes activos de un usuario ordenados por fecha (FIFO).
     */
    @Query("SELECT l FROM Lote l WHERE l.usuarioId = :usuarioId AND l.estado = 'ACTIVO' ORDER BY l.fechaCreacion ASC")
    List<Lote> findLotesActivosDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Busca el lote activo más antiguo de un usuario (FIFO).
     */
    @Query("SELECT l FROM Lote l WHERE l.usuarioId = :usuarioId AND l.estado = 'ACTIVO' ORDER BY l.fechaCreacion ASC LIMIT 1")
    Optional<Lote> findLoteActivoMasAntiguo(@Param("usuarioId") Long usuarioId);
}
