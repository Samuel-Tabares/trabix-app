package com.trabix.billing.repository;

import com.trabix.billing.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
    
    Optional<Lote> findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(Long usuarioId, String estado);
    
    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.tandas WHERE l.id = :id")
    Optional<Lote> findByIdWithTandas(@Param("id") Long id);
    
    @Query("""
        SELECT DISTINCT l FROM Lote l 
        JOIN l.tandas t 
        WHERE l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA' 
        AND t.stockActual <= (t.stockEntregado * 0.2)
        """)
    List<Lote> findLotesConTandasParaCuadre();
}
