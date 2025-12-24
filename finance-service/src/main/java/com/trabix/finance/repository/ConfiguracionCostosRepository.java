package com.trabix.finance.repository;

import com.trabix.finance.entity.ConfiguracionCostos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionCostosRepository extends JpaRepository<ConfiguracionCostos, Long> {

    /**
     * Obtiene la configuración actual (la más reciente por ID).
     */
    Optional<ConfiguracionCostos> findFirstByOrderByIdDesc();

    /**
     * Obtiene la configuración más reciente por fecha de actualización.
     */
    Optional<ConfiguracionCostos> findFirstByOrderByFechaActualizacionDesc();
}
