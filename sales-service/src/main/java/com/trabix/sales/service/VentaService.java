package com.trabix.sales.service;

import com.trabix.common.enums.EstadoVenta;
import com.trabix.common.enums.TipoVenta;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.sales.dto.*;
import com.trabix.sales.entity.Tanda;
import com.trabix.sales.entity.Usuario;
import com.trabix.sales.entity.Venta;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final TandaRepository tandaRepository;
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

    /**
     * Registra una nueva venta.
     */
    @Transactional
    public VentaResponse registrarVenta(Long usuarioId, RegistrarVentaRequest request) {
        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        // Obtener tanda (especificada o activa)
        Tanda tanda;
        if (request.getTandaId() != null) {
            tanda = tandaRepository.findById(request.getTandaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tanda", request.getTandaId()));
        } else {
            tanda = tandaRepository.findTandaActivaDeUsuario(usuarioId)
                    .orElseThrow(() -> new ValidacionNegocioException("No tienes stock disponible"));
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

        // Reducir stock preventivamente
        tanda.reducirStock(request.getCantidad());
        tandaRepository.save(tanda);

        venta = ventaRepository.save(venta);

        log.info("Venta registrada: ID={}, Usuario={}, Tipo={}, Cantidad={}, Total={}",
                venta.getId(), usuario.getCedula(), request.getTipo(),
                request.getCantidad(), precioTotal);

        return mapToResponse(venta);
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

        log.info("Venta aprobada: ID={}", ventaId);
        return mapToResponse(venta);
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

        log.info("Venta rechazada: ID={}, Motivo={}", ventaId, motivo);
        return mapToResponse(venta);
    }

    /**
     * Obtiene una venta por ID.
     */
    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));
        return mapToResponse(venta);
    }

    /**
     * Lista ventas con paginación.
     */
    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentas(Pageable pageable) {
        return ventaRepository.findAll(pageable).map(this::mapToResponse);
    }

    /**
     * Lista ventas pendientes.
     */
    @Transactional(readOnly = true)
    public Page<VentaResponse> listarVentasPendientes(Pageable pageable) {
        return ventaRepository.findByEstado(EstadoVenta.PENDIENTE, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Lista ventas de un usuario.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioIdOrderByFechaRegistroDesc(usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista ventas de una tanda.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentasDeTanda(Long tandaId) {
        return ventaRepository.findByTandaIdOrderByFechaRegistroDesc(tandaId)
                .stream()
                .map(this::mapToResponse)
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
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene resumen de ventas de un usuario.
     */
    @Transactional(readOnly = true)
    public ResumenVentasResponse obtenerResumenUsuario(Long usuarioId) {
        // Obtener estadísticas generales
        Object[] stats = ventaRepository.obtenerEstadisticasUsuario(usuarioId);
        long totalVentas = ((Number) stats[0]).longValue();
        int totalUnidades = ((Number) stats[1]).intValue();
        BigDecimal totalRecaudado = (BigDecimal) stats[2];

        // Obtener por tipo
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
            BigDecimal monto = (BigDecimal) row[3];

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

        // Conteos por estado
        long pendientes = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.PENDIENTE);
        long aprobadas = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.APROBADA);
        long rechazadas = ventaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoVenta.RECHAZADA);

        // Promedios
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
            case MAYOR -> BigDecimal.ZERO; // Se calcula diferente, ver calcularPrecioMayor
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

    private VentaResponse mapToResponse(Venta venta) {
        return VentaResponse.builder()
                .id(venta.getId())
                .vendedor(VentaResponse.VendedorInfo.builder()
                        .id(venta.getUsuario().getId())
                        .nombre(venta.getUsuario().getNombre())
                        .cedula(venta.getUsuario().getCedula())
                        .build())
                .tanda(VentaResponse.TandaInfo.builder()
                        .id(venta.getTanda().getId())
                        .loteId(venta.getTanda().getLoteId())
                        .numero(venta.getTanda().getNumero())
                        .build())
                .tipo(venta.getTipo())
                .cantidad(venta.getCantidad())
                .precioUnitario(venta.getPrecioUnitario())
                .precioTotal(venta.getPrecioTotal())
                .estado(venta.getEstado())
                .fechaRegistro(venta.getFechaRegistro())
                .fechaAprobacion(venta.getFechaAprobacion())
                .nota(venta.getNota())
                .build();
    }
}
