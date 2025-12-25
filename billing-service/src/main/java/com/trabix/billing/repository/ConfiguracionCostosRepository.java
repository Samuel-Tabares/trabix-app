package com.trabix.billing.repository;

import com.trabix.billing.entity.ConfiguracionCostos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionCostosRepository extends JpaRepository<ConfiguracionCostos, Long> {

    @Query("SELECT c FROM ConfiguracionCostos c ORDER BY c.id DESC LIMIT 1")
    Optional<ConfiguracionCostos> findCurrent();
}
