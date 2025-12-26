package com.trabix.inventory.repository;

import com.trabix.inventory.entity.StockProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockProduccionRepository extends JpaRepository<StockProduccion, Long> {

    /**
     * Obtiene el registro único de stock de producción.
     * Solo debe existir un registro en esta tabla.
     */
    @Query("SELECT s FROM StockProduccion s ORDER BY s.id ASC LIMIT 1")
    Optional<StockProduccion> findStock();

    /**
     * Verifica si existe el registro de stock.
     */
    @Query("SELECT COUNT(s) > 0 FROM StockProduccion s")
    boolean existeStock();
}
