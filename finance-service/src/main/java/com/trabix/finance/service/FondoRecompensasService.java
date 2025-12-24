package com.trabix.finance.service;

import com.trabix.finance.dto.FondoRecompensasDTO;
import com.trabix.finance.dto.MovimientoFondoDTO;
import com.trabix.finance.entity.FondoRecompensas;
import com.trabix.finance.entity.MovimientoFondo;
import com.trabix.finance.entity.Usuario;
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
 * El fondo se alimenta con $200 por cada TRABIX vendido.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FondoRecompensasService {

    private final FondoRecompensasRepository fondoRepository;
    private final MovimientoFondoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Inicializa el fondo si no existe.
     */
    @PostConstruct
    @Transactional
    public void inicializar() {
        if (fondoRepository.count() == 0) {
            FondoRecompensas fondo = FondoRecompensas.builder()
                    .saldoActual(BigDecimal.ZERO)
                    .build();
            fondoRepository.save(fondo);
            log.info("Fondo de recompensas inicializado con saldo $0");
        }
    }

    /**
     * Obtiene el saldo y resumen del fondo.
     */
    @Transactional(readOnly = true)
    public FondoRecompensasDTO.SaldoResponse obtenerSaldo() {
        FondoRecompensas fondo = obtenerFondo();
        
        BigDecimal totalIngresos = movimientoRepository.sumarTotalIngresos();
        BigDecimal totalEgresos = movimientoRepository.sumarTotalEgresos();
        long totalMovimientos = movimientoRepository.count();

        return FondoRecompensasDTO.SaldoResponse.builder()
                .saldoActual(fondo.getSaldoActual())
                .totalIngresos(totalIngresos)
                .totalEgresos(totalEgresos)
                .totalMovimientos(totalMovimientos)
                .ultimaActualizacion(fondo.getUpdatedAt())
                .build();
    }

    /**
     * Ingresa dinero al fondo.
     * Usado automáticamente por cuadres de lotes.
     */
    @Transactional
    public MovimientoFondoDTO.Response ingresar(FondoRecompensasDTO.IngresoRequest request) {
        FondoRecompensas fondo = obtenerFondo();
        
        BigDecimal nuevoSaldo = fondo.ingresar(request.getMonto());
        fondoRepository.save(fondo);

        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo("INGRESO")
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .saldoPosterior(nuevoSaldo)
                .referenciaId(request.getReferenciaId())
                .referenciaTipo(request.getReferenciaTipo() != null ? 
                        request.getReferenciaTipo() : "MANUAL")
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        log.info("Ingreso al fondo: ${} - {} - Nuevo saldo: ${}",
                request.getMonto(), request.getDescripcion(), nuevoSaldo);

        return mapToResponse(saved);
    }

    /**
     * Retira dinero del fondo (sin beneficiario específico).
     */
    @Transactional
    public MovimientoFondoDTO.Response retirar(FondoRecompensasDTO.RetiroRequest request) {
        FondoRecompensas fondo = obtenerFondo();

        if (!fondo.tieneSaldoSuficiente(request.getMonto())) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente. Disponible: $" + fondo.getSaldoActual());
        }

        BigDecimal nuevoSaldo = fondo.retirar(request.getMonto());
        fondoRepository.save(fondo);

        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo("EGRESO")
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .saldoPosterior(nuevoSaldo)
                .referenciaTipo("RETIRO")
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        log.info("Retiro del fondo: ${} - {} - Nuevo saldo: ${}",
                request.getMonto(), request.getDescripcion(), nuevoSaldo);

        return mapToResponse(saved);
    }

    /**
     * Entrega un premio a un beneficiario específico.
     */
    @Transactional
    public MovimientoFondoDTO.Response premiar(FondoRecompensasDTO.PremioRequest request) {
        FondoRecompensas fondo = obtenerFondo();

        if (!fondo.tieneSaldoSuficiente(request.getMonto())) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente. Disponible: $" + fondo.getSaldoActual());
        }

        Usuario beneficiario = usuarioRepository.findById(request.getBeneficiarioId())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiario no encontrado"));

        BigDecimal nuevoSaldo = fondo.retirar(request.getMonto());
        fondoRepository.save(fondo);

        MovimientoFondo movimiento = MovimientoFondo.builder()
                .fondo(fondo)
                .tipo("EGRESO")
                .monto(request.getMonto())
                .fecha(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .beneficiario(beneficiario)
                .saldoPosterior(nuevoSaldo)
                .referenciaTipo("PREMIO")
                .build();

        MovimientoFondo saved = movimientoRepository.save(movimiento);
        log.info("Premio entregado: ${} a {} ({}) - {} - Nuevo saldo: ${}",
                request.getMonto(), beneficiario.getNombre(), beneficiario.getCedula(),
                request.getDescripcion(), nuevoSaldo);

        return mapToResponse(saved);
    }

    /**
     * Lista movimientos con paginación.
     */
    @Transactional(readOnly = true)
    public MovimientoFondoDTO.ListResponse listarMovimientos(Pageable pageable) {
        FondoRecompensas fondo = obtenerFondo();
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
     * Lista últimos movimientos.
     */
    @Transactional(readOnly = true)
    public List<MovimientoFondoDTO.Response> listarUltimosMovimientos() {
        return movimientoRepository.findTop10ByOrderByFechaDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene resumen de un período.
     */
    @Transactional(readOnly = true)
    public FondoRecompensasDTO.ResumenPeriodo obtenerResumenPeriodo(
            LocalDateTime desde, LocalDateTime hasta) {
        
        BigDecimal ingresos = movimientoRepository.sumarIngresosPeriodo(desde, hasta);
        BigDecimal egresos = movimientoRepository.sumarEgresosPeriodo(desde, hasta);
        
        List<MovimientoFondo> movimientos = 
                movimientoRepository.findByFechaBetweenOrderByFechaDesc(desde, hasta);
        
        long premios = movimientos.stream()
                .filter(m -> m.tieneBeneficiario())
                .count();

        return FondoRecompensasDTO.ResumenPeriodo.builder()
                .desde(desde)
                .hasta(hasta)
                .ingresos(ingresos)
                .egresos(egresos)
                .balance(ingresos.subtract(egresos))
                .cantidadMovimientos((long) movimientos.size())
                .premiosEntregados(premios)
                .build();
    }

    /**
     * Obtiene premios recibidos por un usuario.
     */
    @Transactional(readOnly = true)
    public MovimientoFondoDTO.ResumenBeneficiario obtenerPremiosBeneficiario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        BigDecimal total = movimientoRepository.sumarPremiosPorBeneficiario(usuarioId);
        List<MovimientoFondo> premios = movimientoRepository
                .findByBeneficiarioIdOrderByFechaDesc(usuarioId);

        return MovimientoFondoDTO.ResumenBeneficiario.builder()
                .beneficiarioId(usuarioId)
                .nombre(usuario.getNombre())
                .cedula(usuario.getCedula())
                .totalPremios(total)
                .cantidadPremios((long) premios.size())
                .build();
    }

    /**
     * Ingresa aporte automático por cuadre de lote.
     * Llamado internamente desde sales-service.
     */
    @Transactional
    public void ingresarAporteCuadre(Long cuadreId, BigDecimal monto, int cantidadTrabix) {
        FondoRecompensasDTO.IngresoRequest request = FondoRecompensasDTO.IngresoRequest.builder()
                .monto(monto)
                .descripcion(String.format("Aporte automático por cuadre #%d (%d TRABIX)", 
                        cuadreId, cantidadTrabix))
                .referenciaId(cuadreId)
                .referenciaTipo("CUADRE")
                .build();
        
        ingresar(request);
    }

    private FondoRecompensas obtenerFondo() {
        return fondoRepository.findPrincipal()
                .orElseThrow(() -> new RuntimeException("Fondo de recompensas no inicializado"));
    }

    private MovimientoFondoDTO.Response mapToResponse(MovimientoFondo movimiento) {
        MovimientoFondoDTO.BeneficiarioInfo beneficiarioInfo = null;
        if (movimiento.getBeneficiario() != null) {
            Usuario b = movimiento.getBeneficiario();
            beneficiarioInfo = MovimientoFondoDTO.BeneficiarioInfo.builder()
                    .id(b.getId())
                    .cedula(b.getCedula())
                    .nombre(b.getNombre())
                    .nivel(b.getNivel())
                    .build();
        }

        return MovimientoFondoDTO.Response.builder()
                .id(movimiento.getId())
                .tipo(movimiento.getTipo())
                .monto(movimiento.getMonto())
                .fecha(movimiento.getFecha())
                .descripcion(movimiento.getDescripcion())
                .beneficiario(beneficiarioInfo)
                .saldoPosterior(movimiento.getSaldoPosterior())
                .referenciaId(movimiento.getReferenciaId())
                .referenciaTipo(movimiento.getReferenciaTipo())
                .build();
    }
}
