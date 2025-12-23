package com.trabix.inventory.service;

import com.trabix.common.enums.EstadoLote;
import com.trabix.common.enums.EstadoTanda;
import com.trabix.common.enums.ModeloNegocio;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.inventory.dto.*;
import com.trabix.inventory.entity.Lote;
import com.trabix.inventory.entity.Tanda;
import com.trabix.inventory.entity.Usuario;
import com.trabix.inventory.repository.LoteRepository;
import com.trabix.inventory.repository.TandaRepository;
import com.trabix.inventory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de inventario: lotes, tandas y stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final LoteRepository loteRepository;
    private final TandaRepository tandaRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${trabix.tanda1-porcentaje:40}")
    private int tanda1Porcentaje;

    @Value("${trabix.tanda2-porcentaje:30}")
    private int tanda2Porcentaje;

    @Value("${trabix.tanda3-porcentaje:30}")
    private int tanda3Porcentaje;

    @Value("${trabix.trigger-cuadre-porcentaje:20}")
    private int triggerCuadrePorcentaje;

    @Value("${trabix.costo-percibido-unitario:2400}")
    private double costoPercibidoDefault;

    /**
     * Crea un nuevo lote para un vendedor.
     * Automáticamente crea las 3 tandas y libera la primera.
     */
    @Transactional
    public LoteResponse crearLote(CrearLoteRequest request) {
        // Validar usuario
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getUsuarioId()));

        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new ValidacionNegocioException("El usuario no está activo");
        }

        // Verificar que no tenga un lote activo pendiente
        if (loteRepository.countByUsuarioIdAndEstado(usuario.getId(), EstadoLote.ACTIVO) > 0) {
            throw new ValidacionNegocioException("El vendedor ya tiene un lote activo. Debe completarlo antes de crear otro.");
        }

        // Determinar modelo de negocio según nivel
        ModeloNegocio modelo = "N2".equals(usuario.getNivel()) 
                ? ModeloNegocio.MODELO_60_40 
                : ModeloNegocio.MODELO_50_50;

        // Determinar costo percibido
        BigDecimal costoPercibido = request.getCostoPercibidoUnitario() != null
                ? BigDecimal.valueOf(request.getCostoPercibidoUnitario())
                : BigDecimal.valueOf(costoPercibidoDefault);

        // Crear lote
        Lote lote = Lote.builder()
                .usuario(usuario)
                .cantidadTotal(request.getCantidad())
                .costoPercibidoUnitario(costoPercibido)
                .modelo(modelo)
                .estado(EstadoLote.ACTIVO)
                .tandas(new ArrayList<>())
                .build();

        lote = loteRepository.save(lote);

        // Crear las 3 tandas
        crearTandas(lote, request.getCantidad());

        // Liberar tanda 1 automáticamente
        Tanda tanda1 = lote.getTandas().get(0);
        tanda1.liberar();
        tandaRepository.save(tanda1);

        log.info("Lote creado: ID={}, Usuario={}, Cantidad={}, Modelo={}", 
                lote.getId(), usuario.getCedula(), request.getCantidad(), modelo);

        return mapToLoteResponse(lote);
    }

    /**
     * Crea las 3 tandas para un lote.
     */
    private void crearTandas(Lote lote, int cantidadTotal) {
        // Calcular cantidades (40%, 30%, 30%)
        int cantidad1 = (int) Math.round(cantidadTotal * tanda1Porcentaje / 100.0);
        int cantidad2 = (int) Math.round(cantidadTotal * tanda2Porcentaje / 100.0);
        int cantidad3 = cantidadTotal - cantidad1 - cantidad2; // El resto para evitar errores de redondeo

        // Crear tanda 1
        Tanda tanda1 = Tanda.builder()
                .lote(lote)
                .numero(1)
                .cantidadAsignada(cantidad1)
                .stockEntregado(0)
                .stockActual(0)
                .estado(EstadoTanda.PENDIENTE)
                .build();

        // Crear tanda 2
        Tanda tanda2 = Tanda.builder()
                .lote(lote)
                .numero(2)
                .cantidadAsignada(cantidad2)
                .stockEntregado(0)
                .stockActual(0)
                .estado(EstadoTanda.PENDIENTE)
                .build();

        // Crear tanda 3
        Tanda tanda3 = Tanda.builder()
                .lote(lote)
                .numero(3)
                .cantidadAsignada(cantidad3)
                .stockEntregado(0)
                .stockActual(0)
                .estado(EstadoTanda.PENDIENTE)
                .build();

        tandaRepository.save(tanda1);
        tandaRepository.save(tanda2);
        tandaRepository.save(tanda3);

        lote.getTandas().add(tanda1);
        lote.getTandas().add(tanda2);
        lote.getTandas().add(tanda3);

        log.debug("Tandas creadas: T1={}, T2={}, T3={}", cantidad1, cantidad2, cantidad3);
    }

    /**
     * Obtiene un lote por ID.
     */
    @Transactional(readOnly = true)
    public LoteResponse obtenerLote(Long id) {
        Lote lote = loteRepository.findByIdWithTandas(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", id));
        return mapToLoteResponse(lote);
    }

    /**
     * Lista todos los lotes con paginación.
     */
    @Transactional(readOnly = true)
    public Page<LoteResponse> listarLotes(Pageable pageable) {
        return loteRepository.findAll(pageable).map(this::mapToLoteResponse);
    }

    /**
     * Lista lotes de un usuario.
     */
    @Transactional(readOnly = true)
    public List<LoteResponse> listarLotesDeUsuario(Long usuarioId) {
        return loteRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::mapToLoteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el lote activo de un usuario.
     */
    @Transactional(readOnly = true)
    public LoteResponse obtenerLoteActivoDeUsuario(Long usuarioId) {
        Lote lote = loteRepository.findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(usuarioId, EstadoLote.ACTIVO)
                .orElseThrow(() -> new ValidacionNegocioException("El usuario no tiene un lote activo"));
        return mapToLoteResponse(lote);
    }

    /**
     * Obtiene el stock actual de un usuario.
     */
    @Transactional(readOnly = true)
    public int obtenerStockActualUsuario(Long usuarioId) {
        return tandaRepository.sumarStockActualUsuario(usuarioId);
    }

    /**
     * Reduce el stock de la tanda activa de un usuario.
     * Retorna la tanda afectada.
     */
    @Transactional
    public TandaResponse reducirStock(Long usuarioId, int cantidad) {
        // Buscar tanda activa con stock
        Tanda tanda = tandaRepository.findTandaActualParaVenta(usuarioId)
                .orElseThrow(() -> new ValidacionNegocioException("No hay stock disponible"));

        if (tanda.getStockActual() < cantidad) {
            throw new ValidacionNegocioException(
                    String.format("Stock insuficiente. Disponible: %d, Solicitado: %d", 
                            tanda.getStockActual(), cantidad));
        }

        tanda.reducirStock(cantidad);
        tandaRepository.save(tanda);

        log.debug("Stock reducido: Tanda={}, Cantidad={}, StockRestante={}", 
                tanda.getId(), cantidad, tanda.getStockActual());

        // Verificar si se debe disparar cuadre
        if (tanda.debeTriggerCuadre(triggerCuadrePorcentaje)) {
            log.info("🔔 TRIGGER CUADRE: Tanda {} del lote {} tiene {}% de stock", 
                    tanda.getNumero(), tanda.getLote().getId(), 
                    Math.round(tanda.getPorcentajeStockRestante()));
            // El cuadre real se maneja en billing-service
        }

        return mapToTandaResponse(tanda);
    }

    /**
     * Libera la siguiente tanda de un lote.
     * Se llama después de un cuadre exitoso.
     */
    @Transactional
    public TandaResponse liberarSiguienteTanda(Long loteId) {
        Lote lote = loteRepository.findByIdWithTandas(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));

        // Buscar la siguiente tanda pendiente
        Tanda tandaPendiente = lote.getTandas().stream()
                .filter(t -> t.getEstado() == EstadoTanda.PENDIENTE)
                .findFirst()
                .orElseThrow(() -> new ValidacionNegocioException("No hay más tandas pendientes en este lote"));

        // Verificar que la tanda anterior esté cuadrada
        if (tandaPendiente.getNumero() > 1) {
            Tanda tandaAnterior = lote.getTandas().get(tandaPendiente.getNumero() - 2);
            if (tandaAnterior.getEstado() != EstadoTanda.CUADRADA) {
                throw new ValidacionNegocioException(
                        "La tanda " + tandaAnterior.getNumero() + " debe ser cuadrada antes de liberar la siguiente");
            }
        }

        // Liberar la tanda
        tandaPendiente.liberar();
        tandaRepository.save(tandaPendiente);

        log.info("Tanda liberada: Lote={}, Tanda={}, Stock={}", 
                loteId, tandaPendiente.getNumero(), tandaPendiente.getStockEntregado());

        return mapToTandaResponse(tandaPendiente);
    }

    /**
     * Marca una tanda como en proceso de cuadre.
     */
    @Transactional
    public TandaResponse iniciarCuadreTanda(Long tandaId) {
        Tanda tanda = tandaRepository.findById(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));

        if (tanda.getEstado() != EstadoTanda.LIBERADA) {
            throw new ValidacionNegocioException("Solo se puede iniciar cuadre de una tanda liberada");
        }

        tanda.iniciarCuadre();
        tandaRepository.save(tanda);

        log.info("Cuadre iniciado: Tanda={}", tandaId);
        return mapToTandaResponse(tanda);
    }

    /**
     * Completa el cuadre de una tanda.
     */
    @Transactional
    public TandaResponse completarCuadreTanda(Long tandaId) {
        Tanda tanda = tandaRepository.findById(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));

        if (tanda.getEstado() != EstadoTanda.EN_CUADRE) {
            throw new ValidacionNegocioException("La tanda no está en proceso de cuadre");
        }

        tanda.completarCuadre();
        tandaRepository.save(tanda);

        // Verificar si el lote está completado
        Lote lote = tanda.getLote();
        if (lote.estaCompletado()) {
            lote.setEstado(EstadoLote.COMPLETADO);
            loteRepository.save(lote);
            log.info("🎉 Lote completado: {}", lote.getId());
        }

        log.info("Cuadre completado: Tanda={}", tandaId);
        return mapToTandaResponse(tanda);
    }

    /**
     * Obtiene las tandas de un lote.
     */
    @Transactional(readOnly = true)
    public List<TandaResponse> obtenerTandasDeLote(Long loteId) {
        return tandaRepository.findByLoteIdOrderByNumeroAsc(loteId)
                .stream()
                .map(this::mapToTandaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una tanda por ID.
     */
    @Transactional(readOnly = true)
    public TandaResponse obtenerTanda(Long tandaId) {
        Tanda tanda = tandaRepository.findById(tandaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", tandaId));
        return mapToTandaResponse(tanda);
    }

    /**
     * Lista tandas que requieren cuadre.
     */
    @Transactional(readOnly = true)
    public List<TandaResponse> listarTandasParaCuadre() {
        return tandaRepository.findTandasParaCuadre(triggerCuadrePorcentaje)
                .stream()
                .map(this::mapToTandaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancela un lote (solo si no tiene ventas).
     */
    @Transactional
    public void cancelarLote(Long loteId) {
        Lote lote = loteRepository.findByIdWithTandas(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));

        // Verificar que no tenga ventas (stock entregado != stock actual)
        boolean tieneVentas = lote.getTandas().stream()
                .anyMatch(t -> t.getStockEntregado() > 0 && t.getStockActual() < t.getStockEntregado());

        if (tieneVentas) {
            throw new ValidacionNegocioException("No se puede cancelar un lote con ventas registradas");
        }

        lote.setEstado(EstadoLote.CANCELADO);
        loteRepository.save(lote);

        log.info("Lote cancelado: {}", loteId);
    }

    // === Métodos de mapeo ===

    private LoteResponse mapToLoteResponse(Lote lote) {
        int stockEntregado = lote.getStockEntregadoTotal();
        int stockActual = lote.getStockActualTotal();
        int stockVendido = stockEntregado - stockActual;

        return LoteResponse.builder()
                .id(lote.getId())
                .vendedor(LoteResponse.VendedorInfo.builder()
                        .id(lote.getUsuario().getId())
                        .nombre(lote.getUsuario().getNombre())
                        .cedula(lote.getUsuario().getCedula())
                        .nivel(lote.getUsuario().getNivel())
                        .build())
                .cantidadTotal(lote.getCantidadTotal())
                .costoPercibidoUnitario(lote.getCostoPercibidoUnitario())
                .inversionTotal(lote.calcularInversionVendedor())
                .modelo(lote.getModelo())
                .estado(lote.getEstado())
                .fechaCreacion(lote.getFechaCreacion())
                .stockEntregado(stockEntregado)
                .stockActual(stockActual)
                .stockVendido(stockVendido)
                .porcentajeVendido(stockEntregado > 0 ? (stockVendido * 100.0 / stockEntregado) : 0.0)
                .tandas(lote.getTandas().stream()
                        .map(this::mapToTandaResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private TandaResponse mapToTandaResponse(Tanda tanda) {
        int stockVendido = tanda.getStockEntregado() - tanda.getStockActual();

        // Determinar si puede ser liberada
        boolean puedeSerLiberada = false;
        if (tanda.getEstado() == EstadoTanda.PENDIENTE) {
            if (tanda.getNumero() == 1) {
                puedeSerLiberada = true;
            } else {
                // Verificar que la anterior esté cuadrada
                Lote lote = tanda.getLote();
                if (lote.getTandas() != null && lote.getTandas().size() >= tanda.getNumero() - 1) {
                    Tanda anterior = lote.getTandas().stream()
                            .filter(t -> t.getNumero() == tanda.getNumero() - 1)
                            .findFirst()
                            .orElse(null);
                    puedeSerLiberada = anterior != null && anterior.getEstado() == EstadoTanda.CUADRADA;
                }
            }
        }

        return TandaResponse.builder()
                .id(tanda.getId())
                .loteId(tanda.getLote().getId())
                .numero(tanda.getNumero())
                .descripcion(tanda.getDescripcion())
                .cantidadAsignada(tanda.getCantidadAsignada())
                .stockEntregado(tanda.getStockEntregado())
                .stockActual(tanda.getStockActual())
                .stockVendido(stockVendido)
                .porcentajeRestante(tanda.getPorcentajeStockRestante())
                .estado(tanda.getEstado())
                .fechaLiberacion(tanda.getFechaLiberacion())
                .requiereCuadre(tanda.debeTriggerCuadre(triggerCuadrePorcentaje))
                .puedeSerLiberada(puedeSerLiberada)
                .build();
    }
}
