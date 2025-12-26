package com.trabix.inventory.repository;

import com.trabix.common.enums.EstadoLote;
import com.trabix.inventory.entity.Lote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Lotes por usuario
    List<Lote> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    Page<Lote> findByUsuarioId(Long usuarioId, Pageable pageable);

    // Lotes activos de un usuario
    List<Lote> findByUsuarioIdAndEstado(Long usuarioId, EstadoLote estado);

    // Lote activo actual de un usuario (el más reciente)
    Optional<Lote> findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(Long usuarioId, EstadoLote estado);

    // Lotes por estado
    Page<Lote> findByEstado(EstadoLote estado, Pageable pageable);

    // Buscar lote con tandas (fetch join para evitar N+1)
    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.tandas WHERE l.id = :id")
    Optional<Lote> findByIdWithTandas(@Param("id") Long id);

    // Lotes con tandas pendientes de cuadre (stock <= 20%)
    @Query("""
        SELECT DISTINCT l FROM Lote l 
        JOIN l.tandas t 
        WHERE l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA' 
        AND t.stockActual <= (t.stockEntregado * 0.2)
        """)
    List<Lote> findLotesConTandasParaCuadre();

    // Contar lotes activos de un usuario
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoLote estado);

    // Lotes por estado (sin paginación)
    List<Lote> findByEstado(EstadoLote estado);
}
