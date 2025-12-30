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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de inventario: lotes, tandas y stock.
 * 
 * LÓGICA DE NEGOCIO CORREGIDA:
 * - Inversión SIEMPRE 50/50 entre Samuel y vendedor
 * - < 50 TRABIX = 2 tandas (50% / 50%)
 * - >= 50 TRABIX = 3 tandas (33.3% / 33.3% / 33.3%)
 * - Múltiples lotes activos permitidos
 * - Ventas FIFO (lote más antiguo primero)
 * 
 * TRIGGERS DE CUADRE:
 * - Tanda 1: 20% = solo alerta, cuadre cuando recaudado >= inversión Samuel
 * - Tanda 2 (en 3 tandas): 10% = trigger cuadre
 * - Última tanda: 20% = trigger cuadre, mini-cuadre al 0%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final LoteRepository loteRepository;
    private final TandaRepository tandaRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockProduccionService stockProduccionService;

    @Value("${trabix.costo-percibido-unitario:2400}")
    private double costoPercibidoDefault;

    // Umbral para decidir 2 o 3 tandas
    private static final int UMBRAL_TRES_TANDAS = 50;

    // Porcentajes de alerta y cuadre por tanda
    private static final int TANDA1_ALERTA_PORCENTAJE = 20;
    private static final int TANDA2_CUADRE_PORCENTAJE = 10;
    private static final int TANDA3_CUADRE_PORCENTAJE = 20;

    /**
     * Crea un nuevo lote para un vendedor.
     * Automáticamente crea 2 o 3 tandas según la cantidad y libera la primera.
     */
    @Transactional
    public LoteResponse crearLote(CrearLoteRequest request) {
        // Validar usuario
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getUsuarioId()));

        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new ValidacionNegocioException("El usuario no está activo");
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

        // Calcular inversiones 50/50
        lote.calcularInversiones();

        lote = loteRepository.save(lote);

        // Crear tandas (2 o 3 según cantidad)
        crearTandasDinamicas(lote, request.getCantidad());

        // Liberar tanda 1 automáticamente
        Tanda tanda1 = lote.getTandas().get(0);
        liberarTandaInterna(tanda1, usuario);

        log.info("✅ Lote creado: ID={}, Usuario={}, Cantidad={}, Tandas={}, Modelo={}, InversionTotal={}, InversionSamuel={}, InversionVendedor={}", 
                lote.getId(), usuario.getCedula(), request.getCantidad(), 
                lote.getTandas().size(), modelo, lote.getInversionTotal(),
                lote.getInversionSamuel(), lote.getInversionVendedor());

        return mapToLoteResponse(lote);
    }

    /**
     * Crea 2 o 3 tandas según la cantidad del lote.
     * < 50 TRABIX = 2 tandas (50% / 50%)
     * >= 50 TRABIX = 3 tandas (33.3% / 33.3% / 33.3%)
     */
    private void crearTandasDinamicas(Lote lote, int cantidadTotal) {
        List<Integer> cantidades;

        if (cantidadTotal < UMBRAL_TRES_TANDAS) {
            // 2 tandas: 50% / 50%
            int cantidad1 = cantidadTotal / 2;
            int cantidad2 = cantidadTotal - cantidad1;
            cantidades = List.of(cantidad1, cantidad2);
            log.debug("Lote con 2 tandas: {} + {} = {}", cantidad1, cantidad2, cantidadTotal);
        } else {
            // 3 tandas: 33.3% / 33.3% / 33.3%
            int cantidad1 = cantidadTotal / 3;
            int cantidad2 = cantidadTotal / 3;
            int cantidad3 = cantidadTotal - cantidad1 - cantidad2;
            cantidades = List.of(cantidad1, cantidad2, cantidad3);
            log.debug("Lote con 3 tandas: {} + {} + {} = {}", cantidad1, cantidad2, cantidad3, cantidadTotal);
        }

        for (int i = 0; i < cantidades.size(); i++) {
            Tanda tanda = Tanda.builder()
                    .lote(lote)
                    .numero(i + 1)
                    .cantidadAsignada(cantidades.get(i))
                    .stockEntregado(0)
                    .stockActual(0)
                    .estado(EstadoTanda.PENDIENTE)
                    .excedenteDinero(BigDecimal.ZERO)
                    .excedenteTrabix(0)
                    .totalRecaudado(BigDecimal.ZERO)
                    .build();

            tandaRepository.save(tanda);
            lote.getTandas().add(tanda);
        }
    }

    /**
     * Libera una tanda internamente (actualiza stock de producción).
     */
    private void liberarTandaInterna(Tanda tanda, Usuario usuario) {
        tanda.liberar();
        tandaRepository.save(tanda);
        
        // Notificar al servicio de stock de producción
        stockProduccionService.entregarStockAVendedor(tanda, usuario);
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
     * Obtiene el lote activo más antiguo de un usuario (para FIFO).
     */
    @Transactional(readOnly = true)
    public LoteResponse obtenerLoteActivoDeUsuario(Long usuarioId) {
        // Obtener el lote activo más antiguo (FIFO)
        List<Lote> lotesActivos = loteRepository.findByUsuarioIdAndEstado(usuarioId, EstadoLote.ACTIVO);
        
        if (lotesActivos.isEmpty()) {
            throw new ValidacionNegocioException("El usuario no tiene lotes activos");
        }

        // Ordenar por fecha de creación ascendente (más antiguo primero)
        Lote loteActivo = lotesActivos.stream()
                .min(Comparator.comparing(Lote::getFechaCreacion))
                .get();

        return mapToLoteResponse(loteActivo);
    }

    /**
     * Obtiene todos los lotes activos de un usuario.
     */
    @Transactional(readOnly = true)
    public List<LoteResponse> obtenerLotesActivosDeUsuario(Long usuarioId) {
        return loteRepository.findByUsuarioIdAndEstado(usuarioId, EstadoLote.ACTIVO)
                .stream()
                .sorted(Comparator.comparing(Lote::getFechaCreacion)) // FIFO
                .map(this::mapToLoteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el stock actual de un usuario (suma de todas las tandas liberadas).
     */
    @Transactional(readOnly = true)
    public int obtenerStockActualUsuario(Long usuarioId) {
        return tandaRepository.sumarStockActualUsuario(usuarioId);
    }

    /**
     * Reduce el stock de la tanda activa de un usuario.
     * Usa FIFO: primero agota las tandas del lote más antiguo.
     */
    @Transactional
    public TandaResponse reducirStock(Long usuarioId, int cantidad) {
        // Buscar tanda activa con stock (FIFO por lote y tanda)
        Tanda tanda = tandaRepository.findTandaActualParaVenta(usuarioId)
                .orElseThrow(() -> new ValidacionNegocioException("No hay stock disponible"));

        if (tanda.getStockActual() < cantidad) {
            throw new ValidacionNegocioException(
                    String.format("Stock insuficiente en tanda actual. Disponible: %d, Solicitado: %d", 
                            tanda.getStockActual(), cantidad));
        }

        tanda.reducirStock(cantidad);
        tandaRepository.save(tanda);

        log.debug("Stock reducido: Lote={}, Tanda={}, Cantidad={}, StockRestante={}", 
                tanda.getLote().getId(), tanda.getNumero(), cantidad, tanda.getStockActual());

        // Verificar triggers según número de tanda
        verificarTriggersTanda(tanda);

        return mapToTandaResponse(tanda);
    }

    /**
     * Verifica los triggers de alerta/cuadre según la tanda.
     */
    private void verificarTriggersTanda(Tanda tanda) {
        double porcentajeRestante = tanda.getPorcentajeStockRestante();
        int numeroTanda = tanda.getNumero();
        int totalTandas = tanda.getLote().getTandas().size();

        // Determinar si es la última tanda (puede ser tanda 2 o 3)
        boolean esUltimaTanda = numeroTanda == totalTandas;

        Lote lote = tanda.getLote();

        if (numeroTanda == 1) {
            // Tanda 1: 20% = SOLO ALERTA (cuadre se dispara por recaudado)
            if (porcentajeRestante <= TANDA1_ALERTA_PORCENTAJE && porcentajeRestante > 0) {
                log.info("📢 ALERTA Tanda 1: Lote {} tiene {}% de stock restante", 
                        lote.getId(), Math.round(porcentajeRestante));
            }
            // Verificar si recaudado >= inversión Samuel
            if (tanda.getTotalRecaudado() != null && lote.getInversionSamuel() != null) {
                if (tanda.getTotalRecaudado().compareTo(lote.getInversionSamuel()) >= 0 
                        && !Boolean.TRUE.equals(lote.getInversionSamuelRecuperada())) {
                    log.info("💰 TRIGGER CUADRE T1: Lote {} - Recaudado ${} >= Inversión Samuel ${}", 
                            lote.getId(), tanda.getTotalRecaudado(), lote.getInversionSamuel());
                }
            }
        } else if (!esUltimaTanda) {
            // Tanda intermedia (solo aplica cuando hay 3 tandas): 10% = trigger cuadre
            if (porcentajeRestante <= TANDA2_CUADRE_PORCENTAJE) {
                log.info("🔔 TRIGGER CUADRE Tanda {}: Lote {} tiene {}% de stock", 
                        numeroTanda, lote.getId(), Math.round(porcentajeRestante));
            }
        } else {
            // Última tanda (2 o 3): 20% = trigger cuadre
            if (porcentajeRestante <= TANDA3_CUADRE_PORCENTAJE && porcentajeRestante > 0) {
                log.info("🔔 TRIGGER CUADRE Tanda {} (final): Lote {} tiene {}% de stock", 
                        numeroTanda, lote.getId(), Math.round(porcentajeRestante));
            }
            
            // Mini-cuadre cuando se agota completamente
            if (tanda.getStockActual() == 0) {
                log.info("🏁 MINI-CUADRE FINAL: Lote {} - Tanda {} agotada", 
                        lote.getId(), numeroTanda);
            }
        }
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
            
            // Transferir excedentes de la tanda anterior
            if (tandaAnterior.getStockActual() > 0) {
                tandaPendiente.agregarExcedenteTrabix(tandaAnterior.getStockActual());
                log.info("📦 Excedente de trabix transferido: {} unidades de T{} a T{}", 
                        tandaAnterior.getStockActual(), tandaAnterior.getNumero(), tandaPendiente.getNumero());
            }
        }

        // Liberar la tanda
        liberarTandaInterna(tandaPendiente, lote.getUsuario());

        log.info("✅ Tanda liberada: Lote={}, Tanda={}, Stock={}", 
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

        log.info("📝 Cuadre iniciado: Lote={}, Tanda={}", tanda.getLote().getId(), tandaId);
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

        // Actualizar flags de inversión recuperada
        Lote lote = tanda.getLote();
        int totalTandas = lote.getTandas().size();
        
        if (tanda.getNumero() == 1) {
            lote.marcarInversionSamuelRecuperada();
            loteRepository.save(lote);
            log.info("💰 Inversión Samuel RECUPERADA: Lote {}", lote.getId());
        } else if (tanda.getNumero() == 2) {
            if (totalTandas == 2) {
                // En lotes de 2 tandas, T2 recupera ambas inversiones
                lote.marcarInversionVendedorRecuperada();
                loteRepository.save(lote);
                log.info("💰 Inversión Vendedor RECUPERADA: Lote {}", lote.getId());
            } else {
                // En lotes de 3 tandas, T2 recupera inversión vendedor
                lote.marcarInversionVendedorRecuperada();
                loteRepository.save(lote);
                log.info("💰 Inversión Vendedor RECUPERADA: Lote {}", lote.getId());
            }
        }

        // Verificar si el lote está completado
        if (lote.estaCompletado()) {
            lote.setEstado(EstadoLote.COMPLETADO);
            loteRepository.save(lote);
            log.info("🎉 LOTE COMPLETADO: {} - Todas las tandas cuadradas", lote.getId());
        }

        log.info("✅ Cuadre completado: Lote={}, Tanda={}", lote.getId(), tandaId);
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
     * Considera los porcentajes correctos según número de tanda.
     */
    @Transactional(readOnly = true)
    public List<TandaResponse> listarTandasParaCuadre() {
        // Obtener todas las tandas liberadas
        List<Tanda> tandasLiberadas = tandaRepository.findByEstado(EstadoTanda.LIBERADA);
        
        return tandasLiberadas.stream()
                .filter(t -> {
                    double porcentaje = t.getPorcentajeStockRestante();
                    int totalTandas = t.getLote().getTandas().size();
                    boolean esUltimaTanda = t.getNumero() == totalTandas;
                    
                    if (t.getNumero() == 1) {
                        // Tanda 1: cuadre por recaudado, no por stock
                        Lote lote = t.getLote();
                        return t.getTotalRecaudado() != null && lote.getInversionSamuel() != null
                                && t.getTotalRecaudado().compareTo(lote.getInversionSamuel()) >= 0;
                    } else if (!esUltimaTanda) {
                        // Tanda intermedia: 10%
                        return porcentaje <= TANDA2_CUADRE_PORCENTAJE;
                    } else {
                        // Última tanda: 20%
                        return porcentaje <= TANDA3_CUADRE_PORCENTAJE;
                    }
                })
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

        // Devolver stock al inventario de Samuel
        int stockADevolver = lote.getTandas().stream()
                .filter(t -> t.getEstado() == EstadoTanda.LIBERADA)
                .mapToInt(Tanda::getStockActual)
                .sum();

        if (stockADevolver > 0) {
            stockProduccionService.devolverStock(
                    stockADevolver, 
                    loteId, 
                    lote.getUsuario().getId(),
                    "Cancelación de lote " + loteId);
        }

        lote.setEstado(EstadoLote.CANCELADO);
        loteRepository.save(lote);

        log.info("❌ Lote cancelado: {}. Stock devuelto: {}", loteId, stockADevolver);
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
                        .modeloNegocio(lote.getUsuario().getModeloNegocio())
                        .build())
                .cantidadTotal(lote.getCantidadTotal())
                .costoPercibidoUnitario(lote.getCostoPercibidoUnitario())
                .inversionTotal(lote.getInversionTotal())
                .inversionSamuel(lote.getInversionSamuel())
                .inversionVendedor(lote.getInversionVendedor())
                .inversionSamuelRecuperada(lote.getInversionSamuelRecuperada())
                .inversionVendedorRecuperada(lote.getInversionVendedorRecuperada())
                .hayGanancias(lote.hayGanancias())
                .porcentajeGananciaVendedor(lote.getPorcentajeGananciaVendedor())
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

        // Determinar si requiere cuadre (según número de tanda)
        boolean requiereCuadre = false;
        boolean proximoACuadre = false;
        if (tanda.getEstado() == EstadoTanda.LIBERADA) {
            double porcentaje = tanda.getPorcentajeStockRestante();
            int totalTandas = tanda.getLote().getTandas().size();
            boolean esUltimaTanda = tanda.getNumero() == totalTandas;
            
            if (tanda.getNumero() == 1) {
                // Tanda 1: cuadre por recaudado
                Lote lote = tanda.getLote();
                requiereCuadre = tanda.getTotalRecaudado() != null && lote.getInversionSamuel() != null
                        && tanda.getTotalRecaudado().compareTo(lote.getInversionSamuel()) >= 0;
                proximoACuadre = porcentaje <= TANDA1_ALERTA_PORCENTAJE;
            } else if (!esUltimaTanda) {
                requiereCuadre = porcentaje <= TANDA2_CUADRE_PORCENTAJE;
                proximoACuadre = porcentaje <= TANDA2_CUADRE_PORCENTAJE + 5;
            } else {
                requiereCuadre = porcentaje <= TANDA3_CUADRE_PORCENTAJE;
                proximoACuadre = porcentaje <= TANDA3_CUADRE_PORCENTAJE + 5;
            }
        }

        return TandaResponse.builder()
                .id(tanda.getId())
                .loteId(tanda.getLote().getId())
                .numero(tanda.getNumero())
                .descripcion(getDescripcionTanda(tanda))
                .cantidadAsignada(tanda.getCantidadAsignada())
                .stockEntregado(tanda.getStockEntregado())
                .stockActual(tanda.getStockActual())
                .stockVendido(stockVendido)
                .porcentajeRestante(tanda.getPorcentajeStockRestante())
                .estado(tanda.getEstado())
                .fechaLiberacion(tanda.getFechaLiberacion())
                .excedenteDinero(tanda.getExcedenteDinero())
                .excedenteTrabix(tanda.getExcedenteTrabix())
                .totalRecaudado(tanda.getTotalRecaudado())
                .requiereCuadre(requiereCuadre)
                .puedeSerLiberada(puedeSerLiberada)
                .proximoACuadre(proximoACuadre)
                .build();
    }

    /**
     * Genera descripción de tanda según número y total de tandas.
     */
    private String getDescripcionTanda(Tanda tanda) {
        int numero = tanda.getNumero();
        int total = tanda.getLote().getTandas().size();

        if (total == 2) {
            // 2 tandas
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Recuperar inversión vendedor + Ganancias)";
                default -> "Tanda " + numero;
            };
        } else {
            // 3 tandas
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Recuperar inversión vendedor)";
                case 3 -> "Tanda 3 (Ganancias puras)";
                default -> "Tanda " + numero;
            };
        }
    }
}
