package com.trabix.equipment.repository;

import com.trabix.equipment.entity.StockEquipos;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockEquiposRepository extends JpaRepository<StockEquipos, Long> {

    /**
     * Obtiene el stock (singleton).
     */
    Optional<StockEquipos> findFirstByOrderByIdAsc();

    /**
     * Obtiene el stock con bloqueo pesimista para operaciones de modificación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockEquipos s ORDER BY s.id ASC LIMIT 1")
    Optional<StockEquipos> findFirstForUpdate();

    /**
     * Obtiene solo la cantidad disponible.
     */
    @Query("SELECT s.kitsDisponibles FROM StockEquipos s ORDER BY s.id ASC LIMIT 1")
    Optional<Integer> obtenerDisponibles();

    /**
     * Verifica si existe el registro de stock.
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM StockEquipos s")
    boolean existeStock();
}
