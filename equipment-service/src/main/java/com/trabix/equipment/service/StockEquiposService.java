package com.trabix.equipment.service;

import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.equipment.dto.StockEquiposDTO;
import com.trabix.equipment.entity.StockEquipos;
import com.trabix.equipment.repository.AsignacionEquipoRepository;
import com.trabix.equipment.repository.StockEquiposRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión del stock de equipos (kits nevera + pijama).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEquiposService {

    private final StockEquiposRepository stockRepository;
    private final AsignacionEquipoRepository asignacionRepository;

    /**
     * Obtiene o crea el registro de stock.
     */
    @Transactional
    public StockEquipos obtenerOCrearStock() {
        return stockRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    StockEquipos nuevo = StockEquipos.builder()
                            .kitsDisponibles(0)
                            .totalKitsHistorico(0)
                            .build();
                    return stockRepository.save(nuevo);
                });
    }

    /**
     * Obtiene el stock con bloqueo para modificación.
     */
    @Transactional
    public StockEquipos obtenerStockParaModificar() {
        return stockRepository.findFirstForUpdate()
                .orElseGet(() -> {
                    StockEquipos nuevo = StockEquipos.builder()
                            .kitsDisponibles(0)
                            .totalKitsHistorico(0)
                            .build();
                    return stockRepository.save(nuevo);
                });
    }

    @Transactional(readOnly = true)
    public StockEquiposDTO.Response obtenerResumen() {
        StockEquipos stock = obtenerOCrearStock();
        long asignacionesActivas = asignacionRepository.countAsignacionesActivas();

        return StockEquiposDTO.Response.builder()
                .kitsDisponibles(stock.getKitsDisponibles())
                .totalKitsHistorico(stock.getTotalKitsHistorico())
                .asignacionesActivas(asignacionesActivas)
                .ultimaActualizacion(stock.getUpdatedAt())
                .build();
    }

    @Transactional
    public StockEquiposDTO.Response agregarKits(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionNegocioException("La cantidad debe ser positiva");
        }

        StockEquipos stock = obtenerStockParaModificar();
        stock.agregarKits(cantidad);
        StockEquipos saved = stockRepository.save(stock);

        log.info("Stock aumentado en {} kits. Nuevo total disponible: {}", 
                cantidad, saved.getKitsDisponibles());

        return StockEquiposDTO.Response.builder()
                .kitsDisponibles(saved.getKitsDisponibles())
                .totalKitsHistorico(saved.getTotalKitsHistorico())
                .asignacionesActivas(asignacionRepository.countAsignacionesActivas())
                .ultimaActualizacion(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public StockEquiposDTO.Response ajustarStock(int nuevoValor) {
        if (nuevoValor < 0) {
            throw new ValidacionNegocioException("El stock no puede ser negativo");
        }

        StockEquipos stock = obtenerStockParaModificar();
        int valorAnterior = stock.getKitsDisponibles();
        stock.ajustarStock(nuevoValor);
        StockEquipos saved = stockRepository.save(stock);

        log.info("Stock ajustado manualmente: {} → {}", valorAnterior, nuevoValor);

        return StockEquiposDTO.Response.builder()
                .kitsDisponibles(saved.getKitsDisponibles())
                .totalKitsHistorico(saved.getTotalKitsHistorico())
                .asignacionesActivas(asignacionRepository.countAsignacionesActivas())
                .ultimaActualizacion(saved.getUpdatedAt())
                .build();
    }

    /**
     * Retira un kit del stock (para asignación).
     */
    @Transactional
    public void retirarKit() {
        StockEquipos stock = obtenerStockParaModificar();
        
        if (!stock.hayDisponibles()) {
            throw new ValidacionNegocioException(
                    "No hay kits disponibles en stock. Stock actual: 0");
        }
        
        stock.retirarKit();
        stockRepository.save(stock);
        log.debug("Kit retirado del stock. Disponibles: {}", stock.getKitsDisponibles());
    }

    /**
     * Devuelve un kit al stock (devolución o reposición).
     */
    @Transactional
    public void devolverKit() {
        StockEquipos stock = obtenerStockParaModificar();
        stock.devolverKit();
        stockRepository.save(stock);
        log.debug("Kit devuelto al stock. Disponibles: {}", stock.getKitsDisponibles());
    }

    /**
     * Verifica si hay kits disponibles.
     */
    @Transactional(readOnly = true)
    public boolean hayDisponibles() {
        return stockRepository.obtenerDisponibles()
                .map(d -> d > 0)
                .orElse(false);
    }

    /**
     * Obtiene la cantidad de kits disponibles.
     */
    @Transactional(readOnly = true)
    public int obtenerDisponibles() {
        return stockRepository.obtenerDisponibles().orElse(0);
    }
}
