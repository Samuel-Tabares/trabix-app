package com.trabix.finance.repository;

import com.trabix.finance.entity.FondoRecompensas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface FondoRecompensasRepository extends JpaRepository<FondoRecompensas, Long> {

    /**
     * Obtiene el fondo principal (solo hay uno).
     */
    Optional<FondoRecompensas> findFirstByOrderByIdAsc();

    /**
     * Obtiene el saldo actual del fondo.
     */
    @Query("SELECT f.saldoActual FROM FondoRecompensas f ORDER BY f.id ASC")
    Optional<BigDecimal> obtenerSaldoActual();
}
