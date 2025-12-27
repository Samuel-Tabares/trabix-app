package com.trabix.finance.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.finance.dto.FondoRecompensasDTO;
import com.trabix.finance.dto.MovimientoFondoDTO;
import com.trabix.finance.entity.*;
import com.trabix.finance.repository.FondoRecompensasRepository;
import com.trabix.finance.repository.MovimientoFondoRepository;
import com.trabix.finance.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del Fondo de Recompensas.
 * 
 * REGLA DE NEGOCIO CRÍTICA:
 * El fondo se alimenta SOLO cuando un VENDEDOR paga un lote.
 * El dinero del ADMIN/dueño NUNCA va al fondo.
 * Por cada TRABIX del lote se agregan $200 (configurable).
 * 
 * Todo se gestiona manualmente por el ADMIN.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FondoRecompensasService {

    private final FondoRecompensasRepository fondoRepository;
    private final MovimientoFondoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    @PostConstruct
    @Transactional
    public void inicializar() {
        if (fondoRepository.count() == 0) {
            FondoRecompensas fondo = FondoRecompensas.builder()
                    .saldoActual(BigDecimal.ZERO)
                    .totalIngresosHistorico(BigDecimal.ZERO)
                    .totalEgresosHistorico(BigDecimal.ZERO)
                    .totalMovimientos(0L)
                    .build();
            fondoRepository.save(fondo);
            log.info("Fondo de recompensas inicializado con saldo $0");
        }
    }

    /**
     * Obtiene el saldo actual del fondo.
     */
    @Transactional(readOnly = true)
    public FondoRecompensasDTO.SaldoResponse obtenerSaldo() {
        FondoRecompensas fondo = obtenerFondoLectura();
        
        return FondoRecompensasDTO.SaldoResponse.builder()
                .saldoActual(fondo.getSaldoActual())
                .totalIngresos(fondo.getTotalIngresosHistorico())
                .totalEgresos(fondo.getTotalEgresosHistorico())
                .totalMovimientos(fondo.getTotalMovimientos())
                .ultimaActualizacion(fondo.getUpdatedAt())
                .build();
    }

    /**
     * Registra un ingreso al fondo.
     * Se usa cuando un VENDEDOR paga un lote.
     * 
     * @param request Datos del ingreso (monto, vendedorId, cantidadTrabix, descripcion)
     */
    @Transactional
    public MovimientoFondoDTO.Response ingresar(FondoRecompensasDTO.IngresoRequest request) {
        // Bloqueo pesimista para evitar race conditions
        FondoRecompensas fondo = obtenerFondoParaActualizar();
        
        // Validar vendedor origen si se proporciona
        Usuario vendedorOrigen = null;
        if (request.getVendedorId() != null) {
            vendedorOrigen = usuarioRepository.findById(request.getVendedorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Vendedor", request.getVendedorId()));
            
            // Validar que sea vendedor (no admin)
            if (vendedorOrigen.esAdmin()) {
                throw new ValidacionNegocioException("El ADMIN/dueño no aporta al fondo. Solo los vendedores.");
            }
        }

        // Realizar el ingreso
        BigDecimal nuevoSaldo = fondo.ingresar(request.getMonto());
        fondoRepository.save(fondo);

        // Determinar tipo de referencia
        ReferenciaMovimiento referenciaTipo = request.getReferenciaTipo();
        if (referenciaTipo == null) {
            referenciaTipo = vendedorOrigen != null ? ReferenciaMovimiento.PAGO_LOTE : ReferenciaMovimiento.OTRO;
        }

        // Crear movimiento
        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo(TipoMovimientoFondo.INGRESO)
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .vendedorOrigen(vendedorOrigen)
                .saldoPosterior(nuevoSaldo)
                .referenciaId(request.getReferenciaId())
                .referenciaTipo(referenciaTipo)
                .cantidadTrabix(request.getCantidadTrabix())
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        
        if (vendedorOrigen != null) {
            log.info("Ingreso al fondo: ${} por {} ({}) - {} TRABIX - Nuevo saldo: ${}",
                    request.getMonto(), vendedorOrigen.getNombre(), vendedorOrigen.getCedula(),
                    request.getCantidadTrabix(), nuevoSaldo);
        } else {
            log.info("Ingreso al fondo: ${} - {} - Nuevo saldo: ${}",
                    request.getMonto(), request.getDescripcion(), nuevoSaldo);
        }

        return mapToResponse(saved);
    }

    /**
     * Retira dinero del fondo (egreso sin beneficiario específico).
     */
    @Transactional
    public MovimientoFondoDTO.Response retirar(FondoRecompensasDTO.RetiroRequest request) {
        FondoRecompensas fondo = obtenerFondoParaActualizar();

        if (!fondo.tieneSaldoSuficiente(request.getMonto())) {
            throw new ValidacionNegocioException(
                    String.format("Saldo insuficiente. Disponible: $%s, Solicitado: $%s",
                            fondo.getSaldoActual(), request.getMonto())
            );
        }

        BigDecimal nuevoSaldo = fondo.retirar(request.getMonto());
        fondoRepository.save(fondo);

        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo(TipoMovimientoFondo.EGRESO)
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .saldoPosterior(nuevoSaldo)
                .referenciaTipo(ReferenciaMovimiento.RETIRO)
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        log.info("Retiro del fondo: ${} - {} - Nuevo saldo: ${}",
                request.getMonto(), request.getDescripcion(), nuevoSaldo);

        return mapToResponse(saved);
    }

    /**
     * Entrega un premio a un beneficiario.
     */
    @Transactional
    public MovimientoFondoDTO.Response premiar(FondoRecompensasDTO.PremioRequest request) {
        FondoRecompensas fondo = obtenerFondoParaActualizar();

        if (!fondo.tieneSaldoSuficiente(request.getMonto())) {
            throw new ValidacionNegocioException(
                    String.format("Saldo insuficiente. Disponible: $%s, Solicitado: $%s",
                            fondo.getSaldoActual(), request.getMonto())
            );
        }

        Usuario beneficiario = usuarioRepository.findById(request.getBeneficiarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", request.getBeneficiarioId()));

        BigDecimal nuevoSaldo = fondo.retirar(request.getMonto());
        fondoRepository.save(fondo);

        // Tipo de premio (por defecto PREMIO, pero puede ser INCENTIVO o BONIFICACION)
        ReferenciaMovimiento tipoPremio = request.getTipoPremio();
        if (tipoPremio == null) {
            tipoPremio = ReferenciaMovimiento.PREMIO;
        }

        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo(TipoMovimientoFondo.EGRESO)
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .beneficiario(beneficiario)
                .saldoPosterior(nuevoSaldo)
                .referenciaTipo(tipoPremio)
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        log.info("Premio entregado: ${} a {} ({}) - {} - Nuevo saldo: ${}",
                request.getMonto(), beneficiario.getNombre(), beneficiario.getCedula(),
                request.getDescripcion(), nuevoSaldo);

        return mapToResponse(saved);
    }

    /**
     * Lista movimientos del fondo paginados.
     */
    @Transactional(readOnly = true)
    public MovimientoFondoDTO.ListResponse listarMovimientos(Pageable pageable) {
        FondoRecompensas fondo = obtenerFondoLectura();
        Page<MovimientoFondo> page = movimientoRepository.findByFondoId(fondo.getId(), pageable);

        List<MovimientoFondoDTO.Response> movimientos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return MovimientoFondoDTO.ListResponse.builder()
                .movimientos(movimientos)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }

    /**
     * Lista los últimos 10 movimientos.
     */
    @Transactional(readOnly = true)
    public List<MovimientoFondoDTO.Response> listarUltimosMovimientos() {
        return movimientoRepository.findTop10ByOrderByFechaDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene resumen del fondo en un período.
     */
    @Transactional(readOnly = true)
    public FondoRecompensasDTO.ResumenPeriodo obtenerResumenPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal ingresos = movimientoRepository.sumarIngresosPeriodo(desde, hasta);
        BigDecimal egresos = movimientoRepository.sumarEgresosPeriodo(desde, hasta);
        long totalMovimientos = movimientoRepository.countByFechaBetween(desde, hasta);
        long premios = movimientoRepository.contarPremiosPeriodo(desde, hasta);
        long pagosLote = movimientoRepository.contarPagosLotePeriodo(desde, hasta);

        return FondoRecompensasDTO.ResumenPeriodo.builder()
                .desde(desde)
                .hasta(hasta)
                .ingresos(ingresos)
                .egresos(egresos)
                .balance(ingresos.subtract(egresos))
                .cantidadMovimientos(totalMovimientos)
                .premiosEntregados(premios)
                .pagosLoteRecibidos(pagosLote)
                .build();
    }

    /**
     * Obtiene resumen de premios de un beneficiario.
     */
    @Transactional(readOnly = true)
    public MovimientoFondoDTO.ResumenBeneficiario obtenerPremiosBeneficiario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        BigDecimal total = movimientoRepository.sumarPremiosPorBeneficiario(usuarioId);
        long cantidad = movimientoRepository.contarPremiosPorBeneficiario(usuarioId);

        return MovimientoFondoDTO.ResumenBeneficiario.builder()
                .beneficiarioId(usuarioId)
                .nombre(usuario.getNombre())
                .cedula(usuario.getCedula())
                .totalPremios(total)
                .cantidadPremios(cantidad)
                .build();
    }

    /**
     * Obtiene resumen de aportes de un vendedor al fondo.
     */
    @Transactional(readOnly = true)
    public MovimientoFondoDTO.ResumenVendedor obtenerAportesVendedor(Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vendedor", vendedorId));

        BigDecimal totalAportado = movimientoRepository.sumarIngresosPorVendedor(vendedorId);
        Long totalTrabix = movimientoRepository.sumarTrabixPorVendedor(vendedorId);
        List<MovimientoFondo> pagos = movimientoRepository.findByVendedorOrigenIdOrderByFechaDesc(vendedorId);

        return MovimientoFondoDTO.ResumenVendedor.builder()
                .vendedorId(vendedorId)
                .nombre(vendedor.getNombre())
                .cedula(vendedor.getCedula())
                .totalAportado(totalAportado)
                .totalTrabix(totalTrabix != null ? totalTrabix : 0L)
                .cantidadPagos((long) pagos.size())
                .build();
    }

    /**
     * Obtiene el fondo para operaciones de solo lectura.
     */
    private FondoRecompensas obtenerFondoLectura() {
        return fondoRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("FondoRecompensas", "default"));
    }

    /**
     * Obtiene el fondo con bloqueo pesimista para actualizaciones.
     */
    private FondoRecompensas obtenerFondoParaActualizar() {
        return fondoRepository.findFirstForUpdate()
                .orElseThrow(() -> new RecursoNoEncontradoException("FondoRecompensas", "default"));
    }

    private MovimientoFondoDTO.Response mapToResponse(MovimientoFondo movimiento) {
        MovimientoFondoDTO.UsuarioInfo beneficiarioInfo = null;
        if (movimiento.getBeneficiario() != null) {
            Usuario b = movimiento.getBeneficiario();
            beneficiarioInfo = MovimientoFondoDTO.UsuarioInfo.builder()
                    .id(b.getId())
                    .cedula(b.getCedula())
                    .nombre(b.getNombre())
                    .nivel(b.getNivel())
                    .build();
        }

        MovimientoFondoDTO.UsuarioInfo vendedorOrigenInfo = null;
        if (movimiento.getVendedorOrigen() != null) {
            Usuario v = movimiento.getVendedorOrigen();
            vendedorOrigenInfo = MovimientoFondoDTO.UsuarioInfo.builder()
                    .id(v.getId())
                    .cedula(v.getCedula())
                    .nombre(v.getNombre())
                    .nivel(v.getNivel())
                    .build();
        }

        return MovimientoFondoDTO.Response.builder()
                .id(movimiento.getId())
                .tipo(movimiento.getTipo())
                .tipoDescripcion(movimiento.getTipo().getDescripcion())
                .monto(movimiento.getMonto())
                .fecha(movimiento.getFecha())
                .descripcion(movimiento.getDescripcion())
                .beneficiario(beneficiarioInfo)
                .vendedorOrigen(vendedorOrigenInfo)
                .saldoPosterior(movimiento.getSaldoPosterior())
                .referenciaId(movimiento.getReferenciaId())
                .referenciaTipo(movimiento.getReferenciaTipo())
                .referenciaTipoDescripcion(movimiento.getReferenciaTipo() != null ? 
                        movimiento.getReferenciaTipo().getDescripcion() : null)
                .cantidadTrabix(movimiento.getCantidadTrabix())
                .createdAt(movimiento.getCreatedAt())
                .build();
    }
}
