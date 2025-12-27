package com.trabix.finance.repository;

import com.trabix.finance.entity.FondoRecompensas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface FondoRecompensasRepository extends JpaRepository<FondoRecompensas, Long> {

    /**
     * Obtiene el fondo principal (singleton).
     * Solo debe existir un registro.
     */
    Optional<FondoRecompensas> findFirstByOrderByIdAsc();

    /**
     * Obtiene el fondo con bloqueo pesimista para operaciones de saldo.
     * CRÍTICO: Usar este método para TODAS las operaciones que modifiquen el saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FondoRecompensas f ORDER BY f.id ASC LIMIT 1")
    Optional<FondoRecompensas> findFirstForUpdate();

    /**
     * Obtiene solo el saldo actual sin cargar toda la entidad.
     * Útil para consultas de solo lectura.
     */
    @Query("SELECT f.saldoActual FROM FondoRecompensas f ORDER BY f.id ASC LIMIT 1")
    Optional<BigDecimal> obtenerSaldoActual();

    /**
     * Verifica si existe algún fondo.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FondoRecompensas f")
    boolean existeFondo();

    /**
     * Verifica si hay saldo suficiente para un monto.
     */
    @Query("SELECT CASE WHEN f.saldoActual >= :monto THEN true ELSE false END FROM FondoRecompensas f ORDER BY f.id ASC LIMIT 1")
    boolean tieneSaldoSuficiente(@Param("monto") BigDecimal monto);
}
