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
 * LÓGICA DE NEGOCIO CORREGIDA:
 * - parteVendedor/parteSamuel: División del recaudado (NO son ganancias hasta recuperar inversión)
 * - esGanancia: true solo cuando AMBAS inversiones están recuperadas
 * - Ventas usan FIFO (lote más antiguo primero)
 * - Stock se reduce al registrar (preventivo)
 * - Stock se restaura si se rechaza
 * 
 * PRECIOS:
 * - UNIDAD: $8,000 (con licor)
 * - PROMO: $12,000 total (2 unidades x $6,000 c/u)
 * - SIN_LICOR: $7,000
 * - REGALO: $0 (máximo 8% del stock del lote)
 * - MAYOR_CON_LICOR: >20 unidades, precio escalado
 * - MAYOR_SIN_LICOR: >20 unidades, precio escalado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final TandaRepository tandaRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;

    // === PRECIOS FIJOS ===
    private static final BigDecimal PRECIO_UNIDAD = new BigDecimal("8000");
    private static final BigDecimal PRECIO_PROMO_UNITARIO = new BigDecimal("6000");
    private static final BigDecimal PRECIO_SIN_LICOR = new BigDecimal("7000");

    // === PRECIOS AL MAYOR CON LICOR ===
    private static final BigDecimal MAYOR_CON_LICOR_21_49 = new BigDecimal("4900");
    private static final BigDecimal MAYOR_CON_LICOR_50_99 = new BigDecimal("4700");
    private static final BigDecimal MAYOR_CON_LICOR_100_MAS = new BigDecimal("4500");

    // === PRECIOS AL MAYOR SIN LICOR ===
    private static final BigDecimal MAYOR_SIN_LICOR_21_49 = new BigDecimal("4800");
    private static final BigDecimal MAYOR_SIN_LICOR_50_99 = new BigDecimal("4500");
    private static final BigDecimal MAYOR_SIN_LICOR_100_MAS = new BigDecimal("4200");

    // === LÍMITES ===
    private static final int CANTIDAD_MINIMA_MAYOR = 21;
    private static final int LIMITE_REGALOS_PORCENTAJE = 8;

    // === UMBRALES DE CUADRE ===
    private static final int TANDA1_ALERTA_PORCENTAJE = 20;
    private static final int TANDA2_CUADRE_PORCENTAJE = 10;
    private static final int TANDA3_CUADRE_PORCENTAJE = 20;

    /**
     * Registra una nueva venta.
     */
    @Transactional
    public VentaResponse registrarVenta(Long usuarioId, RegistrarVentaRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        Tanda tanda;
        if (request.getTandaId() != null) {
            tanda = tandaRepository.findById(request.getTandaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", request.getTandaId()));
            
            // Validar que la tanda pertenece al usuario
            Lote loteTanda = loteRepository.findById(tanda.getLoteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Lote", tanda.getLoteId()));
            if (!loteTanda.getUsuarioId().equals(usuarioId)) {
                throw new ValidacionNegocioException("La tanda especificada no pertenece a este usuario");
            }
        } else {
            tanda = tandaRepository.findTandaActivaDeUsuario(usuarioId)
                    .orElseThrow(() -> new ValidacionNegocioException("No tienes stock disponible"));
        }

        Lote lote = loteRepository.findById(tanda.getLoteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote", tanda.getLoteId()));

        if (!"ACTIVO".equals(lote.getEstado())) {
            throw new ValidacionNegocioException("El lote no está activo");
        }

        if (tanda.getStockActual() < request.getCantidad()) {
            throw new ValidacionNegocioException(
                    String.format("Stock insuficiente. Disponible: %d, Solicitado: %d",
                            tanda.getStockActual(), request.getCantidad()));
        }

        validarSegunTipo(request, tanda);

        BigDecimal precioUnitario = calcularPrecioUnitario(request.getTipo(), request.getCantidad());
        BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(request.getCantidad()));

        // Determinar si esta venta ya es ganancia (ambas inversiones recuperadas)
        boolean esGanancia = lote.hayGanancias();

        Venta venta = Venta.builder()
                .usuario(usuario)
                .tanda(tanda)
                .tipo(request.getTipo())
                .cantidad(request.getCantidad())
                .precioUnitario(precioUnitario)
                .precioTotal(precioTotal)
                .estado(EstadoVenta.PENDIENTE)
                .nota(request.getNota())
                .esGanancia(esGanancia)
                .build();

        calcularPartes(venta, lote);

        tanda.reducirStock(request.getCantidad());
        tandaRepository.save(tanda);

        venta = ventaRepository.save(venta);

        log.info("📝 Venta registrada: ID={}, Usuario={}, Lote={}, Tanda={}, Tipo={}, Cantidad={}, Total={}, ParteVendedor={}, ParteSamuel={}, EsGanancia={}",
                venta.getId(), usuario.getCedula(), lote.getId(), tanda.getNumero(),
                request.getTipo(), request.getCantidad(), precioTotal,
                venta.getParteVendedor(), venta.getParteSamuel(), esGanancia);

        return mapToResponse(venta, tanda, lote, usuario);
    }

    private void validarSegunTipo(RegistrarVentaRequest request, Tanda tanda) {
        TipoVenta tipo = request.getTipo();
        int cantidad = request.getCantidad();

        switch (tipo) {
            case PROMO -> {
                if (cantidad % 2 != 0) {
                    throw new ValidacionNegocioException("La promo 2x1 requiere cantidad par (2, 4, 6, etc.)");
                }
            }
            case REGALO -> {
                validarLimiteRegalos(tanda, cantidad);
            }
            case MAYOR_CON_LICOR, MAYOR_SIN_LICOR -> {
                if (cantidad < CANTIDAD_MINIMA_MAYOR) {
                    throw new ValidacionNegocioException(
                            String.format("Ventas al mayor requieren mínimo %d unidades. Solicitado: %d",
                                    CANTIDAD_MINIMA_MAYOR, cantidad));
                }
            }
            default -> {}
        }
    }

    private BigDecimal calcularPrecioUnitario(TipoVenta tipo, int cantidad) {
        return switch (tipo) {
            case UNIDAD -> PRECIO_UNIDAD;
            case PROMO -> PRECIO_PROMO_UNITARIO;
            case SIN_LICOR -> PRECIO_SIN_LICOR;
            case REGALO -> BigDecimal.ZERO;
            case MAYOR_CON_LICOR -> calcularPrecioMayorConLicor(cantidad);
            case MAYOR_SIN_LICOR -> calcularPrecioMayorSinLicor(cantidad);
        };
    }

    private BigDecimal calcularPrecioMayorConLicor(int cantidad) {
        if (cantidad >= 100) return MAYOR_CON_LICOR_100_MAS;
        else if (cantidad >= 50) return MAYOR_CON_LICOR_50_99;
        else return MAYOR_CON_LICOR_21_49;
    }

    private BigDecimal calcularPrecioMayorSinLicor(int cantidad) {
        if (cantidad >= 100) return MAYOR_SIN_LICOR_100_MAS;
        else if (cantidad >= 50) return MAYOR_SIN_LICOR_50_99;
        else return MAYOR_SIN_LICOR_21_49;
    }

    /**
     * Calcula las PARTES (no ganancias) de la venta según modelo del lote.
     */
    private void calcularPartes(Venta venta, Lote lote) {
        if (venta.getTipo() == TipoVenta.REGALO) {
            venta.setParteVendedor(BigDecimal.ZERO);
            venta.setParteSamuel(BigDecimal.ZERO);
            venta.setModeloNegocio(lote.getModelo());
            return;
        }

        venta.setModeloNegocio(lote.getModelo());
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();
        int porcentajeSamuel = lote.getPorcentajeSamuel();

        BigDecimal parteVendedor = venta.getPrecioTotal()
                .multiply(BigDecimal.valueOf(porcentajeVendedor))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal parteSamuel = venta.getPrecioTotal()
                .multiply(BigDecimal.valueOf(porcentajeSamuel))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        venta.setParteVendedor(parteVendedor);
        venta.setParteSamuel(parteSamuel);
    }

    private void validarLimiteRegalos(Tanda tanda, int cantidadSolicitada) {
        int regalosPrevios = ventaRepository.contarRegalosPorTanda(tanda.getId());
        int limiteRegalos = (tanda.getStockEntregado() * LIMITE_REGALOS_PORCENTAJE) / 100;
        int regalosDisponibles = Math.max(0, limiteRegalos - regalosPrevios);

        if (cantidadSolicitada > regalosDisponibles) {
            throw new ValidacionNegocioException(
                    String.format("Límite de regalos alcanzado (máx %d%% del stock). Disponible: %d, Solicitado: %d",
                            LIMITE_REGALOS_PORCENTAJE, regalosDisponibles, cantidadSolicitada));
        }
    }

    @Transactional
    public VentaResponse aprobarVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new ValidacionNegocioException("Solo se pueden aprobar ventas pendientes");
        }

        venta.aprobar();
        
        // Actualizar total recaudado de la tanda
        Tanda tanda = venta.getTanda();
        if (venta.generaIngreso()) {
            tanda.agregarRecaudado(venta.getPrecioTotal());
            tandaRepository.save(tanda);
        }
        
        ventaRepository.save(venta);

        verificarTriggersCuadre(tanda);

        log.info("✅ Venta aprobada: ID={}", ventaId);
        
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    /**
     * Verifica triggers de cuadre después de aprobar venta.
     */
    private void verificarTriggersCuadre(Tanda tanda) {
        double porcentajeRestante = tanda.getPorcentajeStockRestante();
        int numeroTanda = tanda.getNumero();
        int totalTandas = tandaRepository.countByLoteId(tanda.getLoteId());
        boolean esUltimaTanda = numeroTanda == totalTandas;

        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        
        if (numeroTanda == 1) {
            // Tanda 1: Verificar si recaudado >= inversión Samuel
            if (lote != null && tanda.getTotalRecaudado() != null && lote.getInversionSamuel() != null) {
                if (tanda.getTotalRecaudado().compareTo(lote.getInversionSamuel()) >= 0) {
                    log.info("💰 TRIGGER CUADRE T1: Lote {} - Recaudado ${} >= Inversión Samuel ${}",
                            tanda.getLoteId(), tanda.getTotalRecaudado(), lote.getInversionSamuel());
                }
            }
            if (porcentajeRestante <= TANDA1_ALERTA_PORCENTAJE && porcentajeRestante > 0) {
                log.info("📢 ALERTA Tanda 1: Lote {} tiene {}% de stock restante.",
                        tanda.getLoteId(), String.format("%.1f", porcentajeRestante));
            }
        } else if (totalTandas == 3 && numeroTanda == 2) {
            // Tanda 2 en lotes de 3: trigger al 10%
            if (porcentajeRestante <= TANDA2_CUADRE_PORCENTAJE) {
                log.info("🔔 TRIGGER CUADRE Tanda 2: Lote {} tiene {}% de stock. ¡Cuadre requerido!",
                        tanda.getLoteId(), String.format("%.1f", porcentajeRestante));
            }
        } else if (esUltimaTanda) {
            // Última tanda: trigger al 20%
            if (porcentajeRestante <= TANDA3_CUADRE_PORCENTAJE && porcentajeRestante > 0) {
                log.info("🔔 TRIGGER CUADRE Tanda {} (final): Lote {} tiene {}% de stock. ¡Cuadre requerido!",
                        numeroTanda, tanda.getLoteId(), String.format("%.1f", porcentajeRestante));
            }
            if (tanda.getStockActual() == 0) {
                log.info("🏁 MINI-CUADRE FINAL: Lote {} - Tanda {} agotada completamente.",
                        tanda.getLoteId(), numeroTanda);
            }
        }
    }

    @Transactional
    public VentaResponse rechazarVenta(Long ventaId, String motivo) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new ValidacionNegocioException("Solo se pueden rechazar ventas pendientes");
        }

        Tanda tanda = venta.getTanda();
        tanda.restaurarStock(venta.getCantidad());
        tandaRepository.save(tanda);

        venta.rechazar(motivo);
        ventaRepository.save(venta);

        log.info("❌ Venta rechazada: ID={}, Motivo={}. Stock restaurado.", ventaId, motivo);
        
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));
        
        Tanda tanda = venta.getTanda();
        Lote lote = loteRepository.findById(tanda.getLoteId()).orElse(null);
        return mapToResponse(venta, tanda, lote, venta.getUsuario());
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentas(Pageable pageable) {
        return ventaRepository.findAll(pageable).map(this::mapToResponseSimple);
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentasPendientes(Pageable pageable) {
        return ventaRepository.findByEstado(EstadoVenta.PENDIENTE, pageable)
                .map(this::mapToResponseSimple);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioIdOrderByFechaRegistroDesc(usuarioId)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeTanda(Long tandaId) {
        return ventaRepository.findByTandaIdOrderByFechaRegistroDesc(tandaId)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasHoy(Long usuarioId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);

        return ventaRepository.findByUsuarioIdAndFechaRegistroBetween(usuarioId, inicioHoy, finHoy)
                .stream()
                .map(this::mapToResponseSimple)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumenVentasResponse obtenerResumenUsuario(Long usuarioId) {
        Object[] stats = ventaRepository.obtenerEstadisticasUsuario(usuarioId);
        long totalVentas = ((Number) stats[0]).longValue();
        int totalUnidades = ((Number) stats[1]).intValue();
        BigDecimal totalRecaudado = stats[2] != null ? (BigDecimal) stats[2] : BigDecimal.ZERO;

        // Partes (no son ganancias)
        BigDecimal totalParteVendedor = ventaRepository.sumarParteVendedor(usuarioId);
        BigDecimal totalParteSamuel = ventaRepository.sumarParteSamuel(usuarioId);
        
        // Ganancias reales (solo donde esGanancia=true)
        BigDecimal gananciaRealVendedor = ventaRepository.sumarGananciaRealVendedor(usuarioId);
        BigDecimal gananciaRealSamuel = ventaRepository.sumarGananciaRealSamuel(usuarioId);
        
        if (totalParteVendedor == null) totalParteVendedor = BigDecimal.ZERO;
        if (totalParteSamuel == null) totalParteSamuel = BigDecimal.ZERO;
        if (gananciaRealVendedor == null) gananciaRealVendedor = BigDecimal.ZERO;
        if (gananciaRealSamuel == null) gananciaRealSamuel = BigDecimal.ZERO;

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
                case MAYOR_CON_LICOR, MAYOR_SIN_LICOR -> {
                    ventasMayor += count;
                    unidadesMayor += unidades;
                    recaudadoMayor = recaudadoMayor.add(monto);
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
                .totalParteVendedor(totalParteVendedor)
                .totalParteSamuel(totalParteSamuel)
                .gananciaRealVendedor(gananciaRealVendedor)
                .gananciaRealSamuel(gananciaRealSamuel)
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

    @Transactional(readOnly = true)
    public int obtenerStockUsuario(Long usuarioId) {
        return tandaRepository.sumarStockActualUsuario(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarVentasPendientes() {
        return ventaRepository.countByEstado(EstadoVenta.PENDIENTE);
    }

    // === MAPPERS ===

    private VentaResponse mapToResponse(Venta venta, Tanda tanda, Lote lote, Usuario usuario) {
        List<Tanda> tandasLote = tandaRepository.findByLoteIdOrderByNumeroAsc(tanda.getLoteId());
        int stockDisponibleLote = tandasLote.stream()
                .filter(t -> "LIBERADA".equals(t.getEstado()))
                .mapToInt(Tanda::getStockActual)
                .sum();

        double porcentaje = tanda.getPorcentajeStockRestante();
        int totalTandas = tandasLote.size();
        boolean esUltimaTanda = tanda.getNumero() == totalTandas;
        boolean proximoACuadre = false;

        if (tanda.getNumero() == 1) {
            proximoACuadre = porcentaje <= TANDA1_ALERTA_PORCENTAJE;
        } else if (totalTandas == 3 && tanda.getNumero() == 2) {
            proximoACuadre = porcentaje <= TANDA2_CUADRE_PORCENTAJE + 5;
        } else if (esUltimaTanda) {
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
                    .inversionTotal(lote.getInversionTotal())
                    .inversionSamuel(lote.getInversionSamuel())
                    .inversionVendedor(lote.getInversionVendedor())
                    .inversionSamuelRecuperada(lote.getInversionSamuelRecuperada())
                    .inversionVendedorRecuperada(lote.getInversionVendedorRecuperada())
                    .build();
        }

        return VentaResponse.builder()
                .id(venta.getId())
                .vendedor(VentaResponse.VendedorInfo.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .cedula(usuario.getCedula())
                        .nivel(usuario.getNivel())
                        .modeloNegocio(usuario.getModeloNegocio())
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
                        .excedenteDinero(tanda.getExcedenteDinero())
                        .excedenteTrabix(tanda.getExcedenteTrabix())
                        .totalRecaudado(tanda.getTotalRecaudado())
                        .proximoACuadre(proximoACuadre)
                        .build())
                .tipo(venta.getTipo())
                .cantidad(venta.getCantidad())
                .precioUnitario(venta.getPrecioUnitario())
                .precioTotal(venta.getPrecioTotal())
                .modeloNegocio(venta.getModeloNegocio())
                .parteVendedor(venta.getParteVendedor())
                .parteSamuel(venta.getParteSamuel())
                .esGanancia(venta.getEsGanancia())
                .estado(venta.getEstado())
                .fechaRegistro(venta.getFechaRegistro())
                .fechaAprobacion(venta.getFechaAprobacion())
                .nota(venta.getNota())
                .build();
    }

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
