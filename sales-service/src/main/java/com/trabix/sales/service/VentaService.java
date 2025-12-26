package com.trabix.sales.service;

import com.trabix.common.enums.EstadoVenta;
import com.trabix.common.enums.TipoVenta;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.sales.dto.*;
import com.trabix.sales.entity.Lote;
import com.trabix.sales.entity.Tanda;
import com.trabix.sales.entity.Usuario;
import com.trabix.sales.entity.Venta;
import com.trabix.sales.repository.LoteRepository;
import com.trabix.sales.repository.TandaRepository;
import com.trabix.sales.repository.UsuarioRepository;
import com.trabix.sales.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de ventas.
 * 
 * LÓGICA DE NEGOCIO:
 * - Ventas usan FIFO (lote más antiguo primero)
 * - Stock se reduce al registrar (preventivo)
 * - Stock se restaura si se rechaza
 * - Ganancias se calculan según modelo del lote (60/40 o 50/50)
 * - Al aprobar se verifica si hay trigger de cuadre
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final TandaRepository tandaRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${trabix.precios.unidad:8000}")
    private int precioUnidad;

    @Value("${trabix.precios.promo:6000}")
    private int precioPromo;

    @Value("${trabix.precios.sin-licor:7000}")
    private int precioSinLicor;

    @Value("${trabix.precios.regalo:0}")
    private int precioRegalo;

    @Value("${trabix.limite-regalos-porcentaje:8}")
    private int limiteRegalosPorcentaje;

    // Umbrales de cuadre
    private static final int TANDA1_ALERTA_PORCENTAJE = 20;
    private static final int TANDA2_CUADRE_PORCENTAJE = 10;
    private static final int TANDA3_CUADRE_PORCENTAJE = 20;

    /**
     * Registra una nueva venta.
     * Usa FIFO: busca la tanda del lote más antiguo primero.
     */
    @Transactional
    public VentaResponse registrarVenta(Long usuarioId, RegistrarVentaRequest request) {
        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        // Obtener tanda (especificada o activa con FIFO)
        Tanda tanda;
        if (request.getTandaId() != null) {
            tanda = tandaRepository.findById(request.getTandaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", request.getTandaId()));
        } else {
            tanda = tandaRepository.findTandaActivaDeUsuario(usuarioId)
                    .orElseThrow(() -> new ValidacionNegocioException("No tienes stock disponible"));
        }

        // Obtener lote
        Lote lote = loteRepository.findById(tanda.getLoteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", tanda.getLoteId()));

        // Validar que el lote esté activo
        if (!"ACTIVO".equals(lote.getEstado())) {
            throw new ValidacionNegocioException("El lote no está activo");
        }

        // Validar stock
        if (tanda.getStockActual() < request.getCantidad()) {
            throw new ValidacionNegocioException(
                    String.format("Stock insuficiente. Disponible: %d, Solicitado: %d",
                            tanda.getStockActual(), request.getCantidad()));
        }

        // Validar límite de regalos
        if (request.getTipo() == TipoVenta.REGALO) {
            validarLimiteRegalos(tanda, request.getCantidad());
        }

        // Validar promo (debe ser múltiplo de 2 para 2x1)
        if (request.getTipo() == TipoVenta.PROMO && request.getCantidad() % 2 != 0) {
            throw new ValidacionNegocioException("La promo 2x1 requiere cantidad par");
        }

        // Validar venta al mayor (requiere precio)
        if (request.getTipo() == TipoVenta.MAYOR) {
            if (request.getPrecioUnitarioMayor() == null || request.getPrecioUnitarioMayor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacionNegocioException("La venta al mayor requiere precio unitario");
            }
        }

        // Calcular precio
        BigDecimal precioUnitario;
        if (request.getTipo() == TipoVenta.MAYOR) {
            precioUnitario = request.getPrecioUnitarioMayor();
        } else {
            precioUnitario = calcularPrecioUnitario(request.getTipo());
        }
        BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(request.getCantidad()));

        // Crear venta
        Venta venta = Venta.builder()
                .usuario(usuario)
                .tanda(tanda)
                .tipo(request.getTipo())
                .cantidad(request.getCantidad())
                .precioUnitario(precioUnitario)
                .precioTotal(precioTotal)
                .estado(EstadoVenta.PENDIENTE)
                .nota(request.getNota())
                .build();

        // Calcular ganancias según modelo del lote
        calcularGanancias(venta, lote);

        // Reducir stock preventivamente
        tanda.reducirStock(request.getCantidad());
        tandaRepository.save(tanda);

        venta = ventaRepository.save(venta);

        log.info("📝 Venta registrada: ID={}, Usuario={}, Lote={}, Tanda={}, Tipo={}, Cantidad={}, Total={}, Ganancia={}, ParaSamuel={}",
                venta.getId(), usuario.getCedula(), lote.getId(), tanda.getNumero(),
                request.getTipo(), request.getCantidad(), precioTotal,
                venta.getGananciaVendedor(), venta.getParteSamuel());

        return mapToResponse(venta, tanda, lote, usuario);
    }

    /**
     * Calcula ganancias de la venta según modelo del lote.
     */
    private void calcularGanancias(Venta venta, Lote lote) {
        if (venta.getTipo() == TipoVenta.REGALO) {
            venta.setGananciaVendedor(BigDecimal.ZERO);
            venta.setParteSamuel(BigDecimal.ZERO);
            venta.setModeloNegocio(lote.getModelo());
            return;
        }

        venta.setModeloNegocio(lote.getModelo());
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();
        int porcentajeSamuel = lote.getPorcentajeSamuel();

        BigDecimal gananciaVendedor = venta.getPrecioTotal()
                .multiply(BigDecimal.valueOf(porcentajeVendedor))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal parteSamuel = venta.getPrecioTotal()
                .multiply(BigDecimal.valueOf(porcentajeSamuel))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        venta.setGananciaVendedor(gananciaVendedor);
        venta.setParteSamuel(parteSamuel);
    }

    /**
     * Aprueba una venta pendiente.
     */
    @Transactional
    public VentaResponse aprobarVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new ValidacionNegocioException("Solo se pueden aprobar ventas pendientes");
        }

        venta.aprobar();
        ventaRepository.save(venta);

        // Verificar triggers de cuadre
        Tanda tanda = venta.getTanda();
        verificarTriggersCuadre(tanda);

        log.info("✅ Venta aprobada: ID={}", ventaId);
        
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    /**
     * Verifica si hay triggers de cuadre después de una venta aprobada.
     */
    private void verificarTriggersCuadre(Tanda tanda) {
        double porcentajeRestante = tanda.getPorcentajeStockRestante();
        int numeroTanda = tanda.getNumero();
        
        // Obtener total de tandas del lote
        int totalTandas = tandaRepository.countByLoteId(tanda.getLoteId());
        boolean esUltimaTanda = numeroTanda == totalTandas;

        if (numeroTanda == 1) {
            if (porcentajeRestante <= TANDA1_ALERTA_PORCENTAJE && porcentajeRestante > 0) {
                log.info("📢 ALERTA Tanda 1: Lote {} tiene {}% de stock. Cuadre pendiente cuando se recupere inversión Samuel.",
                        tanda.getLoteId(), Math.round(porcentajeRestante));
            }
        } else if (!esUltimaTanda) {
            if (porcentajeRestante <= TANDA2_CUADRE_PORCENTAJE) {
                log.info("🔔 TRIGGER CUADRE Tanda {}: Lote {} tiene {}% de stock. ¡Cuadre requerido!",
                        numeroTanda, tanda.getLoteId(), Math.round(porcentajeRestante));
            }
        } else {
            if (porcentajeRestante <= TANDA3_CUADRE_PORCENTAJE && porcentajeRestante > 0) {
                log.info("🔔 TRIGGER CUADRE Tanda {} (final): Lote {} tiene {}% de stock. ¡Cuadre requerido!",
                        numeroTanda, tanda.getLoteId(), Math.round(porcentajeRestante));
            }

            if (tanda.getStockActual() == 0) {
                log.info("🏁 MINI-CUADRE FINAL: Lote {} - Tanda {} agotada completamente.",
                        tanda.getLoteId(), numeroTanda);
            }
        }
    }

    /**
     * Rechaza una venta pendiente (restaura stock).
     */
    @Transactional
    public VentaResponse rechazarVenta(Long ventaId, String motivo) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new ValidacionNegocioException("Solo se pueden rechazar ventas pendientes");
        }

        // Restaurar stock
        Tanda tanda = venta.getTanda();
        tanda.restaurarStock(venta.getCantidad());
        tandaRepository.save(tanda);

        venta.rechazar(motivo);
        ventaRepository.save(venta);

        log.info("❌ Venta rechazada: ID={}, Motivo={}. Stock restaurado.", ventaId, motivo);
        
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    /**
     * Obtiene una venta por ID.
     */
    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));
        
        Tanda tanda = venta.getTanda();
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    /**
     * Lista ventas con paginación.
     */
    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentas(Pageable pageable) {
        return ventaRepository.findAll(pageable).map(this::mapToResponseSimple);
    }

    /**
     * Lista ventas pendientes.
     */
    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentasPendientes(Pageable pageable) {
        return ventaRepository.findByEstado(EstadoVenta.PENDIENTE, pageable)
                .map(this::mapToResponseSimple);
    }

    /**
     * Lista ventas de un usuario.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioIdOrderByFechaRegistroDesc(usuarioId)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    /**
     * Lista ventas de una tanda.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeTanda(Long tandaId) {
        return ventaRepository.findByTandaIdOrderByFechaRegistroDesc(tandaId)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    /**
     * Lista ventas del día de un usuario.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasHoy(Long usuarioId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);

        return ventaRepository.findByUsuarioIdAndFechaRegistroBetween(usuarioId, inicioHoy, finHoy)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene resumen de ventas de un usuario.
     */
    @Transactional(readOnly = true)
    public ResumenVentasResponse obtenerResumenUsuario(Long usuarioId) {
        Object[] stats = ventaRepository.obtenerEstadisticasUsuario(usuarioId);
        long totalVentas = ((Number) stats[0]).longValue();
        int totalUnidades = ((Number) stats[1]).intValue();
        BigDecimal totalRecaudado = stats[2] != null ? (BigDecimal) stats[2] : BigDecimal.ZERO;

        // Ganancias
        BigDecimal totalGananciaVendedor = ventaRepository.sumarGananciaVendedor(usuarioId);
        BigDecimal totalParteSamuel = ventaRepository.sumarParteSamuel(usuarioId);
        
        if (totalGananciaVendedor == null) totalGananciaVendedor = BigDecimal.ZERO;
        if (totalParteSamuel == null) totalParteSamuel = BigDecimal.ZERO;

        // Por tipo
        List<Object[]> statsPorTipo = ventaRepository.obtenerEstadisticasPorTipo(usuarioId);

        int ventasUnidad = 0, unidadesUnidad = 0;
        BigDecimal recaudadoUnidad = BigDecimal.ZERO;
        int ventasPromo = 0, unidadesPromo = 0;
        BigDecimal recaudadoPromo = BigDecimal.ZERO;
        int ventasSinLicor = 0, unidadesSinLicor = 0;
        BigDecimal recaudadoSinLicor = BigDecimal.ZERO;
        int ventasRegalo = 0, unidadesRegalo = 0;
        int ventasMayor = 0, unidadesMayor = 0;
        BigDecimal recaudadoMayor = BigDecimal.ZERO;

        for (Object[] row : statsPorTipo) {
            TipoVenta tipo = (TipoVenta) row[0];
            int count = ((Number) row[1]).intValue();
            int unidades = ((Number) row[2]).intValue();
            BigDecimal monto = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;

            switch (tipo) {
                case UNIDAD -> {
                    ventasUnidad = count;
                    unidadesUnidad = unidades;
                    recaudadoUnidad = monto;
                }
                case PROMO -> {
                    ventasPromo = count;
                    unidadesPromo = unidades;
                    recaudadoPromo = monto;
                }
                case SIN_LICOR -> {
                    ventasSinLicor = count;
                    unidadesSinLicor = unidades;
                    recaudadoSinLicor = monto;
                }
                case REGALO -> {
                    ventasRegalo = count;
                    unidadesRegalo = unidades;
                }
                case MAYOR -> {
                    ventasMayor = count;
                    unidadesMayor = unidades;
                    recaudadoMayor = monto;
                }
            }
        }

        long pendientes = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.PENDIENTE);
        long aprobadas = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.APROBADA);
        long rechazadas = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.RECHAZADA);

        BigDecimal promedioVenta = totalVentas > 0
                ? totalRecaudado.divide(BigDecimal.valueOf(totalVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int unidadesConPrecio = unidadesUnidad + unidadesPromo + unidadesSinLicor + unidadesMayor;
        BigDecimal precioPromedio = unidadesConPrecio > 0
                ? totalRecaudado.divide(BigDecimal.valueOf(unidadesConPrecio), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResumenVentasResponse.builder()
                .totalVentas((int) totalVentas)
                .totalUnidadesVendidas(totalUnidades)
                .totalRecaudado(totalRecaudado)
                .totalGananciaVendedor(totalGananciaVendedor)
                .totalParteSamuel(totalParteSamuel)
                .ventasUnidad(ventasUnidad)
                .unidadesUnidad(unidadesUnidad)
                .recaudadoUnidad(recaudadoUnidad)
                .ventasPromo(ventasPromo)
                .unidadesPromo(unidadesPromo)
                .recaudadoPromo(recaudadoPromo)
                .ventasSinLicor(ventasSinLicor)
                .unidadesSinLicor(unidadesSinLicor)
                .recaudadoSinLicor(recaudadoSinLicor)
                .ventasRegalo(ventasRegalo)
                .unidadesRegalo(unidadesRegalo)
                .ventasMayor(ventasMayor)
                .unidadesMayor(unidadesMayor)
                .recaudadoMayor(recaudadoMayor)
                .ventasPendientes((int) pendientes)
                .ventasAprobadas((int) aprobadas)
                .ventasRechazadas((int) rechazadas)
                .promedioVenta(promedioVenta)
                .precioPromedioUnitario(precioPromedio)
                .build();
    }

    /**
     * Obtiene el stock actual del usuario.
     */
    @Transactional(readOnly = true)
    public int obtenerStockUsuario(Long usuarioId) {
        return tandaRepository.sumarStockActualUsuario(usuarioId);
    }

    /**
     * Cuenta ventas pendientes globales.
     */
    @Transactional(readOnly = true)
    public long contarVentasPendientes() {
        return ventaRepository.countByEstado(EstadoVenta.PENDIENTE);
    }

    // === Métodos privados ===

    private BigDecimal calcularPrecioUnitario(TipoVenta tipo) {
        return switch (tipo) {
            case UNIDAD -> BigDecimal.valueOf(precioUnidad);
            case PROMO -> BigDecimal.valueOf(precioPromo);
            case SIN_LICOR -> BigDecimal.valueOf(precioSinLicor);
            case REGALO -> BigDecimal.ZERO;
            case MAYOR -> BigDecimal.ZERO;
        };
    }

    private void validarLimiteRegalos(Tanda tanda, int cantidadSolicitada) {
        int regalosPrevios = ventaRepository.contarRegalosPorTanda(tanda.getId());
        int limiteRegalos = (tanda.getStockEntregado() * limiteRegalosPorcentaje) / 100;
        int regalosDisponibles = Math.max(0, limiteRegalos - regalosPrevios);

        if (cantidadSolicitada > regalosDisponibles) {
            throw new ValidacionNegocioException(
                    String.format("Límite de regalos alcanzado. Disponible: %d, Solicitado: %d",
                            regalosDisponibles, cantidadSolicitada));
        }
    }

    /**
     * Mapeo completo con lote cargado.
     */
    private VentaResponse mapToResponse(Venta venta, Tanda tanda, Lote lote, Usuario usuario) {
        // Stock disponible en el lote
        List<Tanda> tandasLote = tandaRepository.findByLoteIdOrderByNumeroAsc(tanda.getLoteId());
        int stockDisponibleLote = tandasLote.stream()
                .filter(t -> "LIBERADA".equals(t.getEstado()))
                .mapToInt(Tanda::getStockActual)
                .sum();

        // Determinar si está próximo a cuadre
        double porcentaje = tanda.getPorcentajeStockRestante();
        int totalTandas = tandasLote.size();
        boolean esUltimaTanda = tanda.getNumero() == totalTandas;
        boolean proximoACuadre = false;

        if (tanda.getNumero() == 1) {
            proximoACuadre = porcentaje <= TANDA1_ALERTA_PORCENTAJE;
        } else if (!esUltimaTanda) {
            proximoACuadre = porcentaje <= TANDA2_CUADRE_PORCENTAJE + 5;
        } else {
            proximoACuadre = porcentaje <= TANDA3_CUADRE_PORCENTAJE + 5;
        }

        String descripcionTanda = getDescripcionTanda(tanda.getNumero(), totalTandas);

        VentaResponse.LoteInfo loteInfo = null;
        if (lote != null) {
            loteInfo = VentaResponse.LoteInfo.builder()
                    .id(lote.getId())
                    .cantidadTotal(lote.getCantidadTotal())
                    .modelo(lote.getModelo())
                    .estado(lote.getEstado())
                    .fechaCreacion(lote.getFechaCreacion())
                    .stockDisponible(stockDisponibleLote)
                    .porcentajeGananciaVendedor(lote.getPorcentajeGananciaVendedor())
                    .build();
        }

        return VentaResponse.builder()
                .id(venta.getId())
                .vendedor(VentaResponse.VendedorInfo.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .cedula(usuario.getCedula())
                        .nivel(usuario.getNivel())
                        .build())
                .lote(loteInfo)
                .tanda(VentaResponse.TandaInfo.builder()
                        .id(tanda.getId())
                        .numero(tanda.getNumero())
                        .descripcion(descripcionTanda)
                        .stockActual(tanda.getStockActual())
                        .stockEntregado(tanda.getStockEntregado())
                        .porcentajeRestante(porcentaje)
                        .estado(tanda.getEstado())
                        .proximoACuadre(proximoACuadre)
                        .build())
                .tipo(venta.getTipo())
                .cantidad(venta.getCantidad())
                .precioUnitario(venta.getPrecioUnitario())
                .precioTotal(venta.getPrecioTotal())
                .modeloNegocio(venta.getModeloNegocio())
                .gananciaVendedor(venta.getGananciaVendedor())
                .parteSamuel(venta.getParteSamuel())
                .estado(venta.getEstado())
                .fechaRegistro(venta.getFechaRegistro())
                .fechaAprobacion(venta.getFechaAprobacion())
                .nota(venta.getNota())
                .build();
    }

    /**
     * Mapeo simple para listados (carga entidades relacionadas).
     */
    private VentaResponse mapToResponseSimple(Venta venta) {
        Tanda tanda = venta.getTanda();
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        Usuario usuario = venta.getUsuario();
        return mapToResponse(venta, tanda, lote, usuario);
    }

    private String getDescripcionTanda(int numero, int total) {
        if (total == 2) {
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Recuperar inversión vendedor + Ganancias)";
                default -> "Tanda " + numero;
            };
        } else {
            return switch (numero) {
                case 1 -> "Tanda 1 (Recuperar inversión Samuel)";
                case 2 -> "Tanda 2 (Recuperar inversión vendedor)";
                case 3 -> "Tanda 3 (Ganancias puras)";
                default -> "Tanda " + numero;
            };
        }
    }
}
