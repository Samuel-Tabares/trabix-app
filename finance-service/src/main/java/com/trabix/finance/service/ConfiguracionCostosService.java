package com.trabix.finance.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.finance.dto.ConfiguracionCostosDTO;
import com.trabix.finance.entity.ConfiguracionCostos;
import com.trabix.finance.repository.ConfiguracionCostosRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Servicio para gestión de configuración de costos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionCostosService {

    private final ConfiguracionCostosRepository repository;

    // Valores por defecto
    private static final BigDecimal COSTO_REAL_DEFAULT = new BigDecimal("2000");
    private static final BigDecimal COSTO_PERCIBIDO_DEFAULT = new BigDecimal("2400");
    private static final BigDecimal APORTE_FONDO_DEFAULT = new BigDecimal("200");
    private static final BigDecimal APORTE_GESTION_DEFAULT = new BigDecimal("200");

    /**
     * Inicializa configuración por defecto si no existe.
     */
    @PostConstruct
    @Transactional
    public void inicializar() {
        if (repository.count() == 0) {
            ConfiguracionCostos config = ConfiguracionCostos.builder()
                    .costoRealTrabix(COSTO_REAL_DEFAULT)
                    .costoPercibidoTrabix(COSTO_PERCIBIDO_DEFAULT)
                    .aporteFondoPorTrabix(APORTE_FONDO_DEFAULT)
                    .aporteGestionPorTrabix(APORTE_GESTION_DEFAULT)
                    .build();
            repository.save(config);
            log.info("Configuración de costos inicializada con valores por defecto");
        }
    }

    /**
     * Obtiene la configuración actual (solo admin).
     */
    @Transactional(readOnly = true)
    public ConfiguracionCostosDTO.Response obtenerConfiguracion() {
        ConfiguracionCostos config = repository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de costos no encontrada"));
        return mapToResponse(config);
    }

    /**
     * Vista para vendedores (solo costo percibido).
     */
    @Transactional(readOnly = true)
    public ConfiguracionCostosDTO.VendedorView obtenerVistaVendedor() {
        ConfiguracionCostos config = repository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de costos no encontrada"));
        
        return ConfiguracionCostosDTO.VendedorView.builder()
                .costoPorTrabix(config.getCostoPercibidoTrabix())
                .fechaActualizacion(config.getFechaActualizacion())
                .build();
    }

    /**
     * Actualiza la configuración de costos (solo admin).
     */
    @Transactional
    public ConfiguracionCostosDTO.Response actualizar(ConfiguracionCostosDTO.UpdateRequest request) {
        ConfiguracionCostos config = repository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de costos no encontrada"));

        if (request.getCostoPercibidoTrabix().compareTo(request.getCostoRealTrabix()) < 0) {
            throw new ValidacionNegocioException("El costo percibido no puede ser menor al costo real");
        }

        config.setCostoRealTrabix(request.getCostoRealTrabix());
        config.setCostoPercibidoTrabix(request.getCostoPercibidoTrabix());
        config.setAporteFondoPorTrabix(request.getAporteFondoPorTrabix());
        config.setAporteGestionPorTrabix(request.getAporteGestionPorTrabix());

        ConfiguracionCostos saved = repository.save(config);
        log.info("Configuración de costos actualizada: costo_real={}, costo_percibido={}, aporte_fondo={}, aporte_gestion={}",
                saved.getCostoRealTrabix(), saved.getCostoPercibidoTrabix(),
                saved.getAporteFondoPorTrabix(), saved.getAporteGestionPorTrabix());

        return mapToResponse(saved);
    }

    /**
     * Obtiene el costo percibido actual.
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerCostoPercibido() {
        return repository.findFirstByOrderByIdDesc()
                .map(ConfiguracionCostos::getCostoPercibidoTrabix)
                .orElse(COSTO_PERCIBIDO_DEFAULT);
    }

    /**
     * Calcula aporte al fondo para un lote.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularAporteFondo(int cantidadTrabix) {
        ConfiguracionCostos config = repository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de costos no encontrada"));
        return config.calcularAporteFondoPorLote(cantidadTrabix);
    }

    private ConfiguracionCostosDTO.Response mapToResponse(ConfiguracionCostos config) {
        return ConfiguracionCostosDTO.Response.builder()
                .id(config.getId())
                .costoRealTrabix(config.getCostoRealTrabix())
                .costoPercibidoTrabix(config.getCostoPercibidoTrabix())
                .aporteFondoPorTrabix(config.getAporteFondoPorTrabix())
                .aporteGestionPorTrabix(config.getAporteGestionPorTrabix())
                .diferenciaCosto(config.getDiferenciaCosto())
                .margenTotalPorTrabix(config.getMargenTotalPorTrabix())
                .fechaActualizacion(config.getFechaActualizacion())
                .build();
    }
}
