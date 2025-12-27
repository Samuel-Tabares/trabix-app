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
 * 
 * El costo real y percibido se digitan manualmente por el ADMIN.
 * El aporte al fondo también es configurable.
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
                    .build();
            repository.save(config);
            log.info("Configuración de costos inicializada: costo_real=${}, costo_percibido=${}, aporte_fondo=${}",
                    COSTO_REAL_DEFAULT, COSTO_PERCIBIDO_DEFAULT, APORTE_FONDO_DEFAULT);
        }
    }

    /**
     * Obtiene la configuración actual (solo admin).
     * Muestra todos los valores incluyendo costo real.
     */
    @Transactional(readOnly = true)
    public ConfiguracionCostosDTO.Response obtenerConfiguracion() {
        ConfiguracionCostos config = obtenerConfiguracionActual();
        return mapToResponse(config);
    }

    /**
     * Vista para vendedores (solo costo percibido).
     * NO muestra el costo real.
     */
    @Transactional(readOnly = true)
    public ConfiguracionCostosDTO.VendedorView obtenerVistaVendedor() {
        ConfiguracionCostos config = obtenerConfiguracionActual();
        
        return ConfiguracionCostosDTO.VendedorView.builder()
                .costoPorTrabix(config.getCostoPercibidoTrabix())
                .fechaActualizacion(config.getFechaActualizacion())
                .build();
    }

    /**
     * Actualiza la configuración de costos (solo admin).
     * Validaciones:
     * - Costo real > 0
     * - Costo percibido > 0
     * - Aporte fondo >= 0
     */
    @Transactional
    public ConfiguracionCostosDTO.Response actualizar(ConfiguracionCostosDTO.UpdateRequest request) {
        // Usar bloqueo pesimista para evitar actualizaciones concurrentes
        ConfiguracionCostos config = repository.findFirstForUpdate()
                .orElseThrow(() -> new RecursoNoEncontradoException("ConfiguracionCostos", "default"));

        // Validaciones de negocio
        if (request.getCostoRealTrabix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNegocioException("El costo real debe ser mayor a 0");
        }
        
        if (request.getCostoPercibidoTrabix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNegocioException("El costo percibido debe ser mayor a 0");
        }
        
        if (request.getAporteFondoPorTrabix().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionNegocioException("El aporte al fondo no puede ser negativo");
        }

        // Actualizar valores
        config.setCostoRealTrabix(request.getCostoRealTrabix());
        config.setCostoPercibidoTrabix(request.getCostoPercibidoTrabix());
        config.setAporteFondoPorTrabix(request.getAporteFondoPorTrabix());

        ConfiguracionCostos saved = repository.save(config);
        
        log.info("Configuración de costos actualizada: costo_real=${}, costo_percibido=${}, aporte_fondo={}",
                saved.getCostoRealTrabix(), saved.getCostoPercibidoTrabix(), saved.getAporteFondoPorTrabix());

        return mapToResponse(saved);
    }

    /**
     * Obtiene el costo percibido actual.
     * Usado por otros servicios para calcular costos de lotes.
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerCostoPercibido() {
        return repository.obtenerCostoPercibido()
                .orElse(COSTO_PERCIBIDO_DEFAULT);
    }

    /**
     * Obtiene el aporte al fondo por TRABIX.
     * Usado para calcular cuánto va al fondo cuando un vendedor paga un lote.
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerAporteFondo() {
        return repository.obtenerAporteFondo()
                .orElse(APORTE_FONDO_DEFAULT);
    }

    /**
     * Calcula aporte al fondo para un lote.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularAporteFondo(int cantidadTrabix) {
        if (cantidadTrabix <= 0) {
            return BigDecimal.ZERO;
        }
        ConfiguracionCostos config = obtenerConfiguracionActual();
        return config.calcularAporteFondoPorLote(cantidadTrabix);
    }

    /**
     * Calcula costo total de un lote según costo percibido.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularCostoLote(int cantidadTrabix) {
        if (cantidadTrabix <= 0) {
            return BigDecimal.ZERO;
        }
        ConfiguracionCostos config = obtenerConfiguracionActual();
        return config.calcularCostoLote(cantidadTrabix);
    }

    private ConfiguracionCostos obtenerConfiguracionActual() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("ConfiguracionCostos", "default"));
    }

    private ConfiguracionCostosDTO.Response mapToResponse(ConfiguracionCostos config) {
        return ConfiguracionCostosDTO.Response.builder()
                .id(config.getId())
                .costoRealTrabix(config.getCostoRealTrabix())
                .costoPercibidoTrabix(config.getCostoPercibidoTrabix())
                .aporteFondoPorTrabix(config.getAporteFondoPorTrabix())
                .diferenciaCosto(config.getDiferenciaCosto())
                .fechaActualizacion(config.getFechaActualizacion())
                .build();
    }
}
