package com.trabix.finance.repository;

import com.trabix.finance.entity.ConfiguracionCostos;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ConfiguracionCostosRepository extends JpaRepository<ConfiguracionCostos, Long> {

    /**
     * Obtiene la configuración actual (singleton).
     */
    Optional<ConfiguracionCostos> findFirstByOrderByIdAsc();

    /**
     * Obtiene la configuración con bloqueo pesimista para actualizaciones.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConfiguracionCostos c ORDER BY c.id ASC LIMIT 1")
    Optional<ConfiguracionCostos> findFirstForUpdate();

    /**
     * Obtiene solo el costo percibido (para vendedores).
     */
    @Query("SELECT c.costoPercibidoTrabix FROM ConfiguracionCostos c ORDER BY c.id ASC LIMIT 1")
    Optional<BigDecimal> obtenerCostoPercibido();

    /**
     * Obtiene solo el aporte al fondo por TRABIX.
     */
    @Query("SELECT c.aporteFondoPorTrabix FROM ConfiguracionCostos c ORDER BY c.id ASC LIMIT 1")
    Optional<BigDecimal> obtenerAporteFondo();

    /**
     * Verifica si existe alguna configuración.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ConfiguracionCostos c")
    boolean existeConfiguracion();
}
