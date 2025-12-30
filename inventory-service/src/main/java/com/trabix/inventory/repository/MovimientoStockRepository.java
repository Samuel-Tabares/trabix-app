package com.trabix.inventory.repository;

import com.trabix.inventory.entity.MovimientoStock;
import com.trabix.inventory.entity.MovimientoStock.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /**
     * Movimientos ordenados por fecha descendente.
     */
    Page<MovimientoStock> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    /**
     * Movimientos por tipo.
     */
    List<MovimientoStock> findByTipoOrderByFechaMovimientoDesc(TipoMovimiento tipo);

    /**
     * Movimientos de un lote específico.
     */
    List<MovimientoStock> findByLoteIdOrderByFechaMovimientoDesc(Long loteId);

    /**
     * Movimientos de un usuario (entregas a vendedor).
     */
    List<MovimientoStock> findByUsuarioIdOrderByFechaMovimientoDesc(Long usuarioId);

    /**
     * Movimientos en un rango de fechas.
     */
    List<MovimientoStock> findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            LocalDateTime desde, LocalDateTime hasta);

    /**
     * Últimos N movimientos.
     */
    List<MovimientoStock> findTop10ByOrderByFechaMovimientoDesc();

    /**
     * Suma de producción total.
     */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoStock m WHERE m.tipo = 'PRODUCCION'")
    int sumarProduccionTotal();

    /**
     * Suma de entregas totales.
     */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoStock m WHERE m.tipo = 'ENTREGA'")
    int sumarEntregasTotal();

    /**
     * Suma de ventas directas de Samuel.
     */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoStock m WHERE m.tipo = 'VENTA_DIRECTA'")
    int sumarVentasDirectasTotal();
}
