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

    /**
     * Encuentra tandas T2/T3 que requieren cuadre por porcentaje de stock.
     * NO incluye Tanda 1 (que se cuadra por monto, no por porcentaje).
     * 
     * Lógica:
     * - T2 intermedia (en lotes de 3 tandas): trigger al 10%
     * - T2 final (en lotes de 2 tandas): trigger al 20%
     * - T3 final: trigger al 20%
     */
    @Query("""
        SELECT t FROM Tanda t 
        JOIN FETCH t.lote l
        WHERE t.estado = 'LIBERADA' 
        AND t.numero > 1
        AND t.stockEntregado > 0
        AND (
            (l.cantidadTotal < 50 AND t.numero = 2 AND t.stockActual <= (t.stockEntregado * 20 / 100))
            OR
            (l.cantidadTotal >= 50 AND t.numero = 2 AND t.stockActual <= (t.stockEntregado * 10 / 100))
            OR
            (l.cantidadTotal >= 50 AND t.numero = 3 AND t.stockActual <= (t.stockEntregado * 20 / 100))
        )
        """)
    List<Tanda> findTandasParaCuadrePorStock();

    /**
     * Encuentra Tandas 1 que podrían estar listas para cuadre de inversión.
     * El cuadre real se valida en servicio verificando monto recaudado >= inversión Samuel.
     */
    @Query("""
        SELECT t FROM Tanda t 
        JOIN FETCH t.lote l
        WHERE t.estado = 'LIBERADA' 
        AND t.numero = 1
        AND t.stockEntregado > 0
        """)
    List<Tanda> findTandas1Liberadas();

    /**
     * Encuentra Tandas 1 con alerta de stock (20% o menos).
     * Solo informativo - el cuadre real es por monto recaudado.
     */
    @Query("""
        SELECT t FROM Tanda t 
        JOIN FETCH t.lote l
        WHERE t.estado = 'LIBERADA' 
        AND t.numero = 1
        AND t.stockEntregado > 0
        AND t.stockActual <= (t.stockEntregado * 20 / 100)
        """)
    List<Tanda> findTandas1EnAlerta();
    
    @Query("""
        SELECT t FROM Tanda t 
        JOIN t.lote l 
        WHERE l.usuario.id = :usuarioId 
        AND l.estado = 'ACTIVO' 
        AND t.estado = 'LIBERADA'
        ORDER BY t.numero ASC
        """)
    List<Tanda> findTandasActivasDeUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("""
        SELECT CASE WHEN t.estado = 'CUADRADA' THEN true ELSE false END 
        FROM Tanda t 
        WHERE t.lote.id = :loteId AND t.numero = :numero
        """)
    boolean isTandaCuadrada(@Param("loteId") Long loteId, @Param("numero") int numero);

    /**
     * Cuenta tandas de un lote.
     */
    int countByLoteId(Long loteId);

    /**
     * Busca tanda con lote cargado.
     */
    @Query("SELECT t FROM Tanda t JOIN FETCH t.lote WHERE t.id = :id")
    Optional<Tanda> findByIdWithLote(@Param("id") Long id);
}
