package com.trabix.equipment.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.equipment.dto.AsignacionEquipoDTO;
import com.trabix.equipment.entity.*;
import com.trabix.equipment.repository.AsignacionEquipoRepository;
import com.trabix.equipment.repository.PagoMensualidadRepository;
import com.trabix.equipment.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de asignaciones de equipos (kits nevera + pijama).
 * 
 * REGLAS DE NEGOCIO:
 * - Nevera y pijama SIEMPRE van juntas (es un kit)
 * - Solo 1 kit por vendedor activo
 * - Mensualidad: $10,000/mes por el kit completo
 * - Se paga primero, luego se asigna
 * - Día de cobro = día del primer pago (1-28)
 * - Pagos pendientes bloquean cuadres del vendedor
 * 
 * COSTOS DE REPOSICIÓN:
 * - Nevera: $25,000
 * - Pijama: $55,000
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionEquipoService {

    private final AsignacionEquipoRepository asignacionRepository;
    private final PagoMensualidadRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockEquiposService stockService;

    @Value("${trabix.equipos.costo-reposicion.nevera:25000}")
    private BigDecimal costoReposicionNevera;

    @Value("${trabix.equipos.costo-reposicion.pijama:55000}")
    private BigDecimal costoReposicionPijama;

    @Value("${trabix.equipos.mensualidad:10000}")
    private BigDecimal mensualidad;

    // ==================== ASIGNAR ====================

    /**
     * Asigna un kit (nevera + pijama) a un vendedor.
     * 
     * Flujo:
     * 1. Validar que el usuario exista y esté activo
     * 2. Validar que no tenga un kit activo
     * 3. Validar que haya stock disponible
     * 4. Crear la asignación
     * 5. Crear el primer pago (mes actual) como PAGADO
     * 6. Reducir el stock
     */
    @Transactional
    public AsignacionEquipoDTO.Response asignar(AsignacionEquipoDTO.CreateRequest request) {
        // 1. Validar usuario
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getUsuarioId()));

        if (!usuario.estaActivo()) {
            throw new ValidacionNegocioException("El usuario no está activo");
        }

        if (usuario.esAdmin()) {
            throw new ValidacionNegocioException("No se puede asignar equipos al administrador");
        }

        // 2. Validar que no tenga kit activo
        if (asignacionRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoAsignacion.ACTIVO)) {
            throw new ValidacionNegocioException(
                    "El usuario ya tiene un kit asignado. Solo puede tener 1 kit activo.");
        }

        // Verificar si tiene reposiciones pendientes
        List<AsignacionEquipo> reposicionesPendientes = 
                asignacionRepository.findCanceladasPendientesReposicionByUsuario(usuario.getId());
        if (!reposicionesPendientes.isEmpty()) {
            throw new ValidacionNegocioException(
                    "El usuario tiene reposiciones pendientes de pago. " +
                    "Debe pagar la reposición antes de asignar un nuevo kit.");
        }

        // 3. Validar stock
        if (!stockService.hayDisponibles()) {
            throw new ValidacionNegocioException(
                    "No hay kits disponibles en stock. Stock actual: " + stockService.obtenerDisponibles());
        }

        // 4. Crear asignación
        LocalDateTime ahora = LocalDateTime.now();
        int diaCobroMensual = request.getDiaCobroMensual() != null ? 
                request.getDiaCobroMensual() : Math.min(ahora.getDayOfMonth(), 28);

        AsignacionEquipo asignacion = AsignacionEquipo.builder()
                .usuario(usuario)
                .fechaInicio(ahora)
                .diaCobroMensual(diaCobroMensual)
                .estado(EstadoAsignacion.ACTIVO)
                .numeroSerieNevera(request.getNumeroSerieNevera())
                .numeroSeriePijama(request.getNumeroSeriePijama())
                .descripcion(request.getDescripcion())
                .costoReposicionNevera(costoReposicionNevera)
                .costoReposicionPijama(costoReposicionPijama)
                .build();

        AsignacionEquipo saved = asignacionRepository.save(asignacion);

        // 5. Crear primer pago como PAGADO (ya pagó para entrar)
        crearPrimerPago(saved);

        // 6. Reducir stock
        stockService.retirarKit();

        log.info("Kit asignado a {} ({}) - Día de cobro: {} - Stock restante: {}",
                usuario.getNombre(), usuario.getCedula(), diaCobroMensual,
                stockService.obtenerDisponibles());

        return mapToResponse(saved);
    }

    // ==================== DEVOLVER ====================

    /**
     * Procesa la devolución de un kit.
     * 
     * Requisitos:
     * - La asignación debe estar activa
     * - NO debe tener pagos pendientes
     */
    @Transactional
    public AsignacionEquipoDTO.Response devolver(Long id) {
        AsignacionEquipo asignacion = asignacionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));

        if (!asignacion.puedeDevolver()) {
            throw new ValidacionNegocioException(
                    "No se puede devolver. Estado actual: " + asignacion.getEstado().getNombre());
        }

        // Verificar pagos pendientes
        long pagosPendientes = pagoRepository.countPagosPendientesByAsignacion(id);
        if (pagosPendientes > 0) {
            BigDecimal montoPendiente = pagoRepository.findPagosPendientesByAsignacion(id).stream()
                    .map(PagoMensualidad::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            throw new ValidacionNegocioException(
                    "El usuario tiene " + pagosPendientes + " pago(s) pendiente(s) por $" + montoPendiente + 
                    ". Debe estar al día para devolver el kit.");
        }

        asignacion.devolver();
        AsignacionEquipo saved = asignacionRepository.save(asignacion);

        // Devolver al stock
        stockService.devolverKit();

        log.info("Kit devuelto por {} ({}) - Stock disponible: {}",
                asignacion.getUsuario().getNombre(), asignacion.getUsuario().getCedula(),
                stockService.obtenerDisponibles());

        return mapToResponse(saved);
    }

    // ==================== CANCELAR (PÉRDIDA/DAÑO) ====================

    /**
     * Cancela una asignación por pérdida o daño del equipo.
     * 
     * Flujo:
     * 1. Cancelar la asignación con el motivo
     * 2. Calcular costo de reposición según el motivo
     * 3. Marcar como pendiente de reposición
     * 
     * El vendedor debe pagar la reposición.
     * Una vez pagada y marcada, el admin compra el equipo y se agrega al stock.
     */
    @Transactional
    public AsignacionEquipoDTO.Response cancelar(Long id, AsignacionEquipoDTO.CancelarRequest request) {
        AsignacionEquipo asignacion = asignacionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));

        if (!asignacion.puedeCancelar()) {
            throw new ValidacionNegocioException(
                    "No se puede cancelar. Estado actual: " + asignacion.getEstado().getNombre());
        }

        asignacion.cancelar(request.getMotivo(), request.getNota());
        AsignacionEquipo saved = asignacionRepository.save(asignacion);

        BigDecimal costoReposicion = saved.calcularCostoReposicionPorMotivo();

        log.warn("Kit cancelado para {} ({}) - Motivo: {} - Costo reposición: ${}",
                asignacion.getUsuario().getNombre(), asignacion.getUsuario().getCedula(),
                request.getMotivo().getDescripcion(), costoReposicion);

        return mapToResponse(saved);
    }

    // ==================== CONFIRMAR REPOSICIÓN ====================

    /**
     * Confirma que el vendedor pagó la reposición y el admin compró el nuevo kit.
     * Esto agrega el kit al stock nuevamente.
     */
    @Transactional
    public AsignacionEquipoDTO.Response confirmarReposicion(Long id) {
        AsignacionEquipo asignacion = asignacionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));

        if (!asignacion.estaCancelado()) {
            throw new ValidacionNegocioException(
                    "Solo se puede confirmar reposición de asignaciones canceladas. " +
                    "Estado actual: " + asignacion.getEstado().getNombre());
        }

        if (Boolean.TRUE.equals(asignacion.getReposicionPagada())) {
            throw new ValidacionNegocioException("La reposición ya fue confirmada anteriormente");
        }

        asignacion.marcarReposicionPagada();
        AsignacionEquipo saved = asignacionRepository.save(asignacion);

        // Agregar al stock (el admin ya compró el nuevo kit)
        stockService.devolverKit();

        log.info("Reposición confirmada para {} ({}) - Kit agregado al stock - Stock disponible: {}",
                asignacion.getUsuario().getNombre(), asignacion.getUsuario().getCedula(),
                stockService.obtenerDisponibles());

        return mapToResponse(saved);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public AsignacionEquipoDTO.Response actualizar(Long id, AsignacionEquipoDTO.UpdateRequest request) {
        AsignacionEquipo asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));

        if (request.getNumeroSerieNevera() != null) {
            asignacion.setNumeroSerieNevera(request.getNumeroSerieNevera());
        }
        if (request.getNumeroSeriePijama() != null) {
            asignacion.setNumeroSeriePijama(request.getNumeroSeriePijama());
        }
        if (request.getDescripcion() != null) {
            asignacion.setDescripcion(request.getDescripcion());
        }

        AsignacionEquipo saved = asignacionRepository.save(asignacion);
        log.info("Asignación actualizada: ID={}", saved.getId());

        return mapToResponse(saved);
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.Response obtener(Long id) {
        AsignacionEquipo asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
        return mapToResponse(asignacion);
    }

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.ListResponse listar(Pageable pageable) {
        Page<AsignacionEquipo> page = asignacionRepository.findAll(pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.ListResponse listarPorEstado(EstadoAsignacion estado, Pageable pageable) {
        Page<AsignacionEquipo> page = asignacionRepository.findByEstado(estado, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<AsignacionEquipoDTO.Response> listarPorUsuario(Long usuarioId) {
        return asignacionRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.Response obtenerAsignacionActiva(Long usuarioId) {
        AsignacionEquipo asignacion = asignacionRepository.findAsignacionActivaByUsuario(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación activa", usuarioId));
        return mapToResponse(asignacion);
    }

    @Transactional(readOnly = true)
    public boolean tieneKitActivo(Long usuarioId) {
        return asignacionRepository.existsByUsuarioIdAndEstado(usuarioId, EstadoAsignacion.ACTIVO);
    }

    @Transactional(readOnly = true)
    public List<AsignacionEquipoDTO.Response> listarCanceladasPendientesReposicion() {
        return asignacionRepository.findCanceladasPendientesReposicion().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== RESÚMENES ====================

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.ResumenUsuario obtenerResumenUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        boolean tieneKitActivo = asignacionRepository.existsByUsuarioIdAndEstado(usuarioId, EstadoAsignacion.ACTIVO);
        Long asignacionActivaId = asignacionRepository.findAsignacionActivaByUsuario(usuarioId)
                .map(AsignacionEquipo::getId)
                .orElse(null);

        long pagosPendientes = pagoRepository.countPagosPendientesByUsuario(usuarioId);
        BigDecimal totalPendiente = pagoRepository.sumarMontoPendienteByUsuario(usuarioId);
        long pagosVencidos = pagoRepository.findPagosVencidosByUsuario(usuarioId).size();

        // Bloqueado si tiene pagos pendientes o vencidos
        boolean bloqueado = pagosPendientes > 0;

        return AsignacionEquipoDTO.ResumenUsuario.builder()
                .usuarioId(usuarioId)
                .nombre(usuario.getNombre())
                .cedula(usuario.getCedula())
                .tieneKitActivo(tieneKitActivo)
                .asignacionActivaId(asignacionActivaId)
                .pagosPendientes((int) pagosPendientes)
                .totalPendiente(totalPendiente)
                .pagosVencidos((int) pagosVencidos)
                .bloqueadoPorPagos(bloqueado)
                .build();
    }

    @Transactional(readOnly = true)
    public AsignacionEquipoDTO.ResumenGeneral obtenerResumenGeneral() {
        int stockDisponible = stockService.obtenerDisponibles();
        long activas = asignacionRepository.countByEstado(EstadoAsignacion.ACTIVO);
        long devueltas = asignacionRepository.countByEstado(EstadoAsignacion.DEVUELTO);
        long canceladas = asignacionRepository.countByEstado(EstadoAsignacion.CANCELADO);

        long pagosPendientes = pagoRepository.countByEstado(EstadoPago.PENDIENTE);
        long pagosVencidos = pagoRepository.countByEstado(EstadoPago.VENCIDO);
        BigDecimal montoPendiente = pagoRepository.sumarTotalPendiente();
        BigDecimal montoPagado = pagoRepository.sumarTotalPagado();

        long canceladasPendientesReposicion = asignacionRepository.findCanceladasPendientesReposicion().size();

        return AsignacionEquipoDTO.ResumenGeneral.builder()
                .stockDisponible(stockDisponible)
                .asignacionesActivas(activas)
                .asignacionesDevueltas(devueltas)
                .asignacionesCanceladas(canceladas)
                .pagosPendientes(pagosPendientes)
                .pagosVencidos(pagosVencidos)
                .montoPendiente(montoPendiente)
                .montoPagadoTotal(montoPagado)
                .canceladasPendientesReposicion(canceladasPendientesReposicion)
                .build();
    }

    /**
     * Verifica si un usuario tiene pagos pendientes (para bloquear cuadres).
     * Este método puede ser llamado desde otros servicios.
     */
    @Transactional(readOnly = true)
    public boolean tienePagosPendientes(Long usuarioId) {
        return pagoRepository.countPagosPendientesByUsuario(usuarioId) > 0;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Crea el primer pago como PAGADO (porque ya pagó para entrar).
     */
    private void crearPrimerPago(AsignacionEquipo asignacion) {
        LocalDateTime ahora = LocalDateTime.now();
        int mes = ahora.getMonthValue();
        int anio = ahora.getYear();

        int diaVencimiento = Math.min(asignacion.getDiaCobroMensual(), 
                LocalDate.of(anio, mes, 1).lengthOfMonth());

        PagoMensualidad primerPago = PagoMensualidad.builder()
                .asignacion(asignacion)
                .mes(mes)
                .anio(anio)
                .monto(mensualidad)
                .fechaVencimiento(LocalDate.of(anio, mes, diaVencimiento))
                .estado(EstadoPago.PAGADO)
                .fechaPago(ahora)
                .nota("Primer pago - Ingreso al servicio")
                .build();

        pagoRepository.save(primerPago);
        log.debug("Primer pago creado como PAGADO para asignación {}", asignacion.getId());
    }

    private AsignacionEquipoDTO.ListResponse buildListResponse(Page<AsignacionEquipo> page) {
        List<AsignacionEquipoDTO.Response> asignaciones = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return AsignacionEquipoDTO.ListResponse.builder()
                .asignaciones(asignaciones)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }

    private AsignacionEquipoDTO.Response mapToResponse(AsignacionEquipo a) {
        List<PagoMensualidad> pendientes = pagoRepository.findPagosPendientesByAsignacion(a.getId());
        BigDecimal montoPendiente = pendientes.stream()
                .map(PagoMensualidad::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoReposicionPendiente = BigDecimal.ZERO;
        if (a.estaCancelado() && !Boolean.TRUE.equals(a.getReposicionPagada())) {
            costoReposicionPendiente = a.calcularCostoReposicionPorMotivo();
        }

        return AsignacionEquipoDTO.Response.builder()
                .id(a.getId())
                .usuarioId(a.getUsuario().getId())
                .usuarioNombre(a.getUsuario().getNombre())
                .usuarioCedula(a.getUsuario().getCedula())
                .fechaInicio(a.getFechaInicio())
                .diaCobroMensual(a.getDiaCobroMensual())
                .estado(a.getEstado())
                .estadoDescripcion(a.getEstado().getNombre())
                .numeroSerieNevera(a.getNumeroSerieNevera())
                .numeroSeriePijama(a.getNumeroSeriePijama())
                .descripcion(a.getDescripcion())
                .costoReposicionNevera(a.getCostoReposicionNevera())
                .costoReposicionPijama(a.getCostoReposicionPijama())
                .costoReposicionTotal(a.getCostoReposicionTotal())
                .fechaFinalizacion(a.getFechaFinalizacion())
                .motivoCancelacion(a.getMotivoCancelacion())
                .motivoCancelacionDescripcion(a.getMotivoCancelacion() != null ? 
                        a.getMotivoCancelacion().getDescripcion() : null)
                .notaFinalizacion(a.getNotaFinalizacion())
                .reposicionPagada(a.getReposicionPagada())
                .costoReposicionPendiente(costoReposicionPendiente)
                .pagosPendientes(pendientes.size())
                .montoPendiente(montoPendiente)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
