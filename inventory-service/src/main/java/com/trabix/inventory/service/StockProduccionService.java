package com.trabix.inventory.service;

import com.trabix.common.enums.EstadoLote;
import com.trabix.common.enums.EstadoTanda;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.inventory.dto.*;
import com.trabix.inventory.entity.Lote;
import com.trabix.inventory.entity.MovimientoStock;
import com.trabix.inventory.entity.MovimientoStock.TipoMovimiento;
import com.trabix.inventory.entity.StockProduccion;
import com.trabix.inventory.entity.Tanda;
import com.trabix.inventory.entity.Usuario;
import com.trabix.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del stock de producción de Samuel (N1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockProduccionService {

    private final StockProduccionRepository stockProduccionRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final LoteRepository loteRepository;
    private final TandaRepository tandaRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${trabix.alerta-stock-bajo:300}")
    private int alertaStockBajoDefault;

    @Value("${trabix.costo-real-unitario:1800}")
    private double costoRealDefault;

    /**
     * Inicializa el stock de producción si no existe.
     */
    @Transactional
    public StockProduccion inicializarStock() {
        if (stockProduccionRepository.existeStock()) {
            return stockProduccionRepository.findStock().get();
        }

        StockProduccion stock = StockProduccion.builder()
                .stockProducidoTotal(0)
                .stockDisponible(0)
                .costoRealUnitario(BigDecimal.valueOf(costoRealDefault))
                .nivelAlertaStockBajo(alertaStockBajoDefault)
                .build();

        stock = stockProduccionRepository.save(stock);
        log.info("Stock de producción inicializado");
        return stock;
    }

    /**
     * Obtiene el estado completo del stock de producción.
     */
    @Transactional(readOnly = true)
    public StockProduccionResponse obtenerEstadoStock() {
        StockProduccion stock = obtenerOCrearStock();

        // Calcular reservados (tandas pendientes de todos los lotes activos)
        int totalReservado = calcularTotalReservado();
        int deficit = stock.calcularDeficit(totalReservado);
        
        // Calcular totales
        int totalEntregado = calcularTotalEntregado();
        int totalVentasDirectas = movimientoStockRepository.sumarVentasDirectasTotal();

        // Determinar alertas
        boolean alertaStockBajo = stock.tieneStockBajo();
        boolean alertaDeficit = deficit > 0;
        String mensajeAlerta = construirMensajeAlerta(stock, totalReservado, deficit);

        // Porcentaje de cobertura
        double porcentajeCobertura = totalReservado > 0 
                ? (stock.getStockDisponible() * 100.0 / totalReservado) 
                : 100.0;

        // Resumen por vendedor
        List<StockProduccionResponse.ResumenVendedor> resumenVendedores = calcularResumenPorVendedor();

        return StockProduccionResponse.builder()
                .stockDisponible(stock.getStockDisponible())
                .stockProducidoTotal(stock.getStockProducidoTotal())
                .costoRealUnitario(stock.getCostoRealUnitario())
                .ultimaProduccion(stock.getUltimaProduccion())
                .totalReservado(totalReservado)
                .deficit(deficit)
                .porcentajeCobertura(Math.round(porcentajeCobertura * 100.0) / 100.0)
                .totalEntregado(totalEntregado)
                .totalVentasDirectas(totalVentasDirectas)
                .nivelAlertaStockBajo(stock.getNivelAlertaStockBajo())
                .alertaStockBajo(alertaStockBajo)
                .alertaDeficit(alertaDeficit)
                .mensajeAlerta(mensajeAlerta)
                .resumenPorVendedor(resumenVendedores)
                .build();
    }

    /**
     * Registra nueva producción de TRABIX.
     */
    @Transactional
    public StockProduccionResponse registrarProduccion(RegistrarProduccionRequest request) {
        StockProduccion stock = obtenerOCrearStock();

        BigDecimal costoUnitario = request.getCostoUnitario() != null 
                ? request.getCostoUnitario() 
                : stock.getCostoRealUnitario();

        if (costoUnitario == null) {
            costoUnitario = BigDecimal.valueOf(costoRealDefault);
        }

        // Registrar producción
        stock.registrarProduccion(request.getCantidad(), costoUnitario);
        stockProduccionRepository.save(stock);

        // Registrar movimiento
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(TipoMovimiento.PRODUCCION)
                .cantidad(request.getCantidad())
                .stockResultante(stock.getStockDisponible())
                .costoUnitario(costoUnitario)
                .descripcion(request.getDescripcion() != null 
                        ? request.getDescripcion() 
                        : "Producción de " + request.getCantidad() + " TRABIX")
                .build();
        movimientoStockRepository.save(movimiento);

        log.info("📦 Producción registrada: {} TRABIX a ${}/u. Stock disponible: {}", 
                request.getCantidad(), costoUnitario, stock.getStockDisponible());

        return obtenerEstadoStock();
    }

    /**
     * Registra venta directa de Samuel en eventos.
     */
    @Transactional
    public StockProduccionResponse registrarVentaDirecta(int cantidad, String descripcion) {
        StockProduccion stock = obtenerOCrearStock();

        if (cantidad > stock.getStockDisponible()) {
            throw new ValidacionNegocioException(
                    "Stock insuficiente. Disponible: " + stock.getStockDisponible());
        }

        stock.setStockDisponible(stock.getStockDisponible() - cantidad);
        stockProduccionRepository.save(stock);

        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(TipoMovimiento.VENTA_DIRECTA)
                .cantidad(cantidad)
                .stockResultante(stock.getStockDisponible())
                .descripcion(descripcion != null ? descripcion : "Venta directa en evento")
                .build();
        movimientoStockRepository.save(movimiento);

        log.info("🛒 Venta directa: {} TRABIX. Stock disponible: {}", 
                cantidad, stock.getStockDisponible());

        return obtenerEstadoStock();
    }

    /**
     * Ajuste manual de stock (positivo o negativo).
     */
    @Transactional
    public StockProduccionResponse ajustarStock(int cantidad, String motivo) {
        StockProduccion stock = obtenerOCrearStock();

        int nuevoStock = stock.getStockDisponible() + cantidad;
        if (nuevoStock < 0) {
            throw new ValidacionNegocioException(
                    "El ajuste resultaría en stock negativo. Stock actual: " + stock.getStockDisponible());
        }

        stock.setStockDisponible(nuevoStock);
        if (cantidad > 0) {
            stock.setStockProducidoTotal(stock.getStockProducidoTotal() + cantidad);
        }
        stockProduccionRepository.save(stock);

        TipoMovimiento tipo = cantidad >= 0 ? TipoMovimiento.AJUSTE_POSITIVO : TipoMovimiento.AJUSTE_NEGATIVO;
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(tipo)
                .cantidad(Math.abs(cantidad))
                .stockResultante(stock.getStockDisponible())
                .descripcion(motivo != null ? motivo : "Ajuste manual de stock")
                .build();
        movimientoStockRepository.save(movimiento);

        log.info("⚙️ Ajuste de stock: {} TRABIX. Motivo: {}. Stock disponible: {}", 
                cantidad, motivo, stock.getStockDisponible());

        return obtenerEstadoStock();
    }

    /**
     * Configura el nivel de alerta de stock bajo.
     */
    @Transactional
    public StockProduccionResponse configurarAlertaStockBajo(int nivel) {
        if (nivel < 0) {
            throw new ValidacionNegocioException("El nivel de alerta debe ser positivo");
        }

        StockProduccion stock = obtenerOCrearStock();
        stock.setNivelAlertaStockBajo(nivel);
        stockProduccionRepository.save(stock);

        log.info("Nivel de alerta configurado: {} TRABIX", nivel);
        return obtenerEstadoStock();
    }

    /**
     * Entrega stock a un vendedor (al liberar tanda).
     * Llamado internamente por InventarioService.
     */
    @Transactional
    public void entregarStockAVendedor(Tanda tanda, Usuario usuario) {
        StockProduccion stock = obtenerOCrearStock();
        int cantidad = tanda.getCantidadAsignada();

        // Verificar si hay stock suficiente
        if (cantidad > stock.getStockDisponible()) {
            log.warn("⚠️ DÉFICIT: Se liberó tanda pero no hay stock suficiente. " +
                    "Necesario: {}, Disponible: {}", cantidad, stock.getStockDisponible());
            // No lanzamos excepción porque el sistema permite déficit
            // Solo entregamos lo que hay o marcamos como entrega pendiente
        }

        // Reducir stock disponible (puede quedar en 0 o ya estaba en 0)
        int entregaReal = Math.min(cantidad, stock.getStockDisponible());
        if (entregaReal > 0) {
            stock.setStockDisponible(stock.getStockDisponible() - entregaReal);
            stockProduccionRepository.save(stock);
        }

        // Registrar movimiento
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(TipoMovimiento.ENTREGA)
                .cantidad(cantidad)
                .stockResultante(stock.getStockDisponible())
                .loteId(tanda.getLote().getId())
                .usuarioId(usuario.getId())
                .descripcion("Entrega Tanda " + tanda.getNumero() + " a " + usuario.getNombre())
                .build();
        movimientoStockRepository.save(movimiento);

        log.info("📤 Entrega a {}: {} TRABIX (Tanda {}). Stock disponible: {}", 
                usuario.getNombre(), cantidad, tanda.getNumero(), stock.getStockDisponible());
    }

    /**
     * Devuelve stock (ej: cancelación de lote).
     */
    @Transactional
    public void devolverStock(int cantidad, Long loteId, Long usuarioId, String motivo) {
        StockProduccion stock = obtenerOCrearStock();
        
        stock.devolverStock(cantidad);
        stockProduccionRepository.save(stock);

        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(TipoMovimiento.DEVOLUCION)
                .cantidad(cantidad)
                .stockResultante(stock.getStockDisponible())
                .loteId(loteId)
                .usuarioId(usuarioId)
                .descripcion(motivo != null ? motivo : "Devolución de stock")
                .build();
        movimientoStockRepository.save(movimiento);

        log.info("📥 Devolución: {} TRABIX. Stock disponible: {}", cantidad, stock.getStockDisponible());
    }

    /**
     * Lista el historial de movimientos.
     */
    @Transactional(readOnly = true)
    public Page<MovimientoStockResponse> listarMovimientos(Pageable pageable) {
        return movimientoStockRepository.findAllByOrderByFechaMovimientoDesc(pageable)
                .map(this::mapToMovimientoResponse);
    }

    /**
     * Obtiene los últimos movimientos.
     */
    @Transactional(readOnly = true)
    public List<MovimientoStockResponse> obtenerUltimosMovimientos() {
        return movimientoStockRepository.findTop10ByOrderByFechaMovimientoDesc()
                .stream()
                .map(this::mapToMovimientoResponse)
                .collect(Collectors.toList());
    }

    // === MÉTODOS PRIVADOS ===

    private StockProduccion obtenerOCrearStock() {
        return stockProduccionRepository.findStock()
                .orElseGet(this::inicializarStock);
    }

    /**
     * Calcula el total de TRABIX reservados (tandas pendientes de todos los lotes activos).
     */
    private int calcularTotalReservado() {
        List<Lote> lotesActivos = loteRepository.findByEstado(EstadoLote.ACTIVO);
        return lotesActivos.stream()
                .flatMap(l -> l.getTandas().stream())
                .filter(t -> t.getEstado() == EstadoTanda.PENDIENTE)
                .mapToInt(Tanda::getCantidadAsignada)
                .sum();
    }

    /**
     * Calcula el total entregado (tandas liberadas).
     */
    private int calcularTotalEntregado() {
        return movimientoStockRepository.sumarEntregasTotal();
    }

    /**
     * Construye mensaje de alerta si aplica.
     */
    private String construirMensajeAlerta(StockProduccion stock, int totalReservado, int deficit) {
        List<String> alertas = new ArrayList<>();

        if (deficit > 0) {
            alertas.add(String.format("🚨 DÉFICIT: Debes %d TRABIX a vendedores (Reservado: %d, Disponible: %d)", 
                    deficit, totalReservado, stock.getStockDisponible()));
        }

        if (stock.tieneStockBajo() && deficit == 0) {
            alertas.add(String.format("⚠️ STOCK BAJO: Solo quedan %d TRABIX (alerta en %d)", 
                    stock.getStockDisponible(), stock.getNivelAlertaStockBajo()));
        }

        return alertas.isEmpty() ? null : String.join(" | ", alertas);
    }

    /**
     * Calcula resumen de stock por vendedor.
     */
    private List<StockProduccionResponse.ResumenVendedor> calcularResumenPorVendedor() {
        List<Lote> lotesActivos = loteRepository.findByEstado(EstadoLote.ACTIVO);
        
        // Agrupar por usuario
        Map<Long, List<Lote>> lotesPorUsuario = lotesActivos.stream()
                .collect(Collectors.groupingBy(l -> l.getUsuario().getId()));

        List<StockProduccionResponse.ResumenVendedor> resultado = new ArrayList<>();

        for (Map.Entry<Long, List<Lote>> entry : lotesPorUsuario.entrySet()) {
            List<Lote> lotesUsuario = entry.getValue();
            Usuario usuario = lotesUsuario.get(0).getUsuario();

            int stockPedido = 0;
            int stockReservado = 0;
            int stockEnMano = 0;
            int stockSalido = 0;

            for (Lote lote : lotesUsuario) {
                stockPedido += lote.getCantidadTotal();
                
                for (Tanda tanda : lote.getTandas()) {
                    if (tanda.getEstado() == EstadoTanda.PENDIENTE) {
                        stockReservado += tanda.getCantidadAsignada();
                    } else if (tanda.getEstado() == EstadoTanda.LIBERADA || 
                               tanda.getEstado() == EstadoTanda.EN_CUADRE) {
                        stockEnMano += tanda.getStockActual();
                        stockSalido += (tanda.getStockEntregado() - tanda.getStockActual());
                    } else if (tanda.getEstado() == EstadoTanda.CUADRADA) {
                        stockSalido += tanda.getStockEntregado();
                    }
                }
            }

            resultado.add(StockProduccionResponse.ResumenVendedor.builder()
                    .usuarioId(usuario.getId())
                    .nombre(usuario.getNombre())
                    .nivel(usuario.getNivel())
                    .stockPedido(stockPedido)
                    .stockReservado(stockReservado)
                    .stockEnMano(stockEnMano)
                    .stockSalido(stockSalido)
                    .lotesActivos(lotesUsuario.size())
                    .build());
        }

        return resultado;
    }

    private MovimientoStockResponse mapToMovimientoResponse(MovimientoStock mov) {
        String nombreUsuario = null;
        if (mov.getUsuarioId() != null) {
            nombreUsuario = usuarioRepository.findById(mov.getUsuarioId())
                    .map(Usuario::getNombre)
                    .orElse(null);
        }

        return MovimientoStockResponse.builder()
                .id(mov.getId())
                .tipo(mov.getTipo())
                .cantidad(mov.getCantidad())
                .stockResultante(mov.getStockResultante())
                .costoUnitario(mov.getCostoUnitario())
                .loteId(mov.getLoteId())
                .usuarioId(mov.getUsuarioId())
                .nombreUsuario(nombreUsuario)
                .descripcion(mov.getDescripcion())
                .fechaMovimiento(mov.getFechaMovimiento())
                .build();
    }
}
