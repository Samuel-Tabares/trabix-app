package com.trabix.notification.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.notification.dto.NotificacionDTO;
import com.trabix.notification.entity.Notificacion;
import com.trabix.notification.entity.TipoNotificacion;
import com.trabix.notification.entity.Usuario;
import com.trabix.notification.repository.NotificacionRepository;
import com.trabix.notification.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de notificaciones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;
    private final UsuarioRepository usuarioRepository;

    @Value("${trabix.notificaciones.dias-limpieza:30}")
    private int diasLimpieza;

    // ==================== CREAR ====================

    @Transactional
    public NotificacionDTO.Response crear(NotificacionDTO.CreateRequest request) {
        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getUsuarioId()));
        }

        Notificacion notificacion = Notificacion.builder()
                .usuario(usuario)
                .tipo(request.getTipo() != null ? request.getTipo() : TipoNotificacion.INFO)
                .titulo(request.getTitulo())
                .mensaje(request.getMensaje())
                .referenciaTipo(request.getReferenciaTipo())
                .referenciaId(request.getReferenciaId())
                .leida(false)
                .build();

        Notificacion saved = repository.save(notificacion);
        
        if (usuario != null) {
            log.info("Notificación creada para {}: {} - {}", 
                    usuario.getNombre(), saved.getTipo(), saved.getTitulo());
        } else {
            log.info("Notificación broadcast creada: {} - {}", saved.getTipo(), saved.getTitulo());
        }

        return mapToResponse(saved);
    }

    @Transactional
    public NotificacionDTO.Response crearBroadcast(NotificacionDTO.BroadcastRequest request) {
        Notificacion notificacion = Notificacion.builder()
                .usuario(null) // Broadcast = sin usuario específico
                .tipo(request.getTipo() != null ? request.getTipo() : TipoNotificacion.INFO)
                .titulo(request.getTitulo())
                .mensaje(request.getMensaje())
                .leida(false)
                .build();

        Notificacion saved = repository.save(notificacion);
        log.info("Notificación BROADCAST creada: {} - {}", saved.getTipo(), saved.getTitulo());

        return mapToResponse(saved);
    }

    // ==================== MARCAR LEÍDA ====================

    @Transactional
    public NotificacionDTO.Response marcarLeida(Long id) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación", id));

        notificacion.marcarLeida();
        Notificacion saved = repository.save(notificacion);

        return mapToResponse(saved);
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        int actualizadas = repository.marcarTodasLeidasByUsuario(usuarioId, LocalDateTime.now());
        log.info("Marcadas {} notificaciones como leídas para usuario {}", actualizadas, usuarioId);
        return actualizadas;
    }

    @Transactional
    public int marcarLeidasPorIds(List<Long> ids) {
        int actualizadas = repository.marcarLeidasByIds(ids, LocalDateTime.now());
        log.info("Marcadas {} notificaciones como leídas", actualizadas);
        return actualizadas;
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Notificación", id);
        }
        repository.deleteById(id);
        log.info("Notificación eliminada: {}", id);
    }

    @Transactional
    public int eliminarLeidasDeUsuario(Long usuarioId) {
        int eliminadas = repository.eliminarLeidasByUsuario(usuarioId);
        log.info("Eliminadas {} notificaciones leídas del usuario {}", eliminadas, usuarioId);
        return eliminadas;
    }

    @Transactional
    public int limpiarAntiguas(int diasAntiguedad) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
        int eliminadas = repository.eliminarAntiguasLeidasMayorA(fechaLimite);
        log.info("Eliminadas {} notificaciones leídas anteriores a {}", eliminadas, fechaLimite);
        return eliminadas;
    }

    /**
     * Limpieza automática de notificaciones antiguas.
     * Ejecuta diariamente a las 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void limpiezaAutomatica() {
        log.info("Ejecutando limpieza automática de notificaciones...");
        int eliminadas = limpiarAntiguas(diasLimpieza);
        log.info("Limpieza automática completada: {} notificaciones eliminadas", eliminadas);
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public NotificacionDTO.Response obtener(Long id) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación", id));
        return mapToResponse(notificacion);
    }

    @Transactional(readOnly = true)
    public NotificacionDTO.ListResponse listarPorUsuario(Long usuarioId, Pageable pageable) {
        Page<Notificacion> page = repository.findByUsuarioIdOrBroadcast(usuarioId, pageable);
        long noLeidas = repository.countNoLeidasByUsuario(usuarioId);
        return buildListResponse(page, noLeidas);
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO.Response> listarNoLeidasPorUsuario(Long usuarioId) {
        return repository.findNoLeidasByUsuario(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO.Response> listarRecientesPorUsuario(Long usuarioId) {
        return repository.findTop10ByUsuario(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificacionDTO.ListResponse listarTodas(Pageable pageable) {
        Page<Notificacion> page = repository.findAll(pageable);
        long noLeidas = repository.countByLeida(false);
        return buildListResponse(page, noLeidas);
    }

    @Transactional(readOnly = true)
    public NotificacionDTO.ListResponse listarBroadcasts(Pageable pageable) {
        Page<Notificacion> page = repository.findBroadcasts(pageable);
        return buildListResponse(page, 0);
    }

    @Transactional(readOnly = true)
    public NotificacionDTO.ContadorResponse contarPorUsuario(Long usuarioId) {
        long noLeidas = repository.countNoLeidasByUsuario(usuarioId);
        long total = repository.countByUsuarioIdOrBroadcast(usuarioId);

        return NotificacionDTO.ContadorResponse.builder()
                .total(total)
                .noLeidas(noLeidas)
                .leidas(total - noLeidas)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificacionDTO.ResumenTipos obtenerResumenTipos() {
        List<Object[]> conteos = repository.contarPorTipo();
        
        NotificacionDTO.ResumenTipos resumen = new NotificacionDTO.ResumenTipos();
        long total = 0;
        
        for (Object[] row : conteos) {
            TipoNotificacion tipo = (TipoNotificacion) row[0];
            Long count = (Long) row[1];
            total += count;
            
            switch (tipo) {
                case INFO -> resumen.setInfo(count);
                case ALERTA -> resumen.setAlerta(count);
                case RECORDATORIO -> resumen.setRecordatorio(count);
                case SISTEMA -> resumen.setSistema(count);
                case EXITO -> resumen.setExito(count);
                case ERROR -> resumen.setError(count);
            }
        }
        
        resumen.setTotal(total);
        return resumen;
    }

    // ==================== MÉTODOS DE UTILIDAD PARA OTROS SERVICIOS ====================

    /**
     * Crea una notificación simple para un usuario.
     */
    @Transactional
    public void notificar(Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje) {
        NotificacionDTO.CreateRequest request = NotificacionDTO.CreateRequest.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .build();
        crear(request);
    }

    /**
     * Crea una notificación con referencia.
     */
    @Transactional
    public void notificarConReferencia(Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
                                       String referenciaTipo, Long referenciaId) {
        // Evitar duplicados
        if (repository.existsByUsuarioIdAndReferenciaTipoAndReferenciaId(usuarioId, referenciaTipo, referenciaId)) {
            log.debug("Notificación duplicada evitada: {} {} para usuario {}", referenciaTipo, referenciaId, usuarioId);
            return;
        }
        
        NotificacionDTO.CreateRequest request = NotificacionDTO.CreateRequest.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .build();
        crear(request);
    }

    /**
     * Envía notificación broadcast a todos.
     */
    @Transactional
    public void notificarATodos(TipoNotificacion tipo, String titulo, String mensaje) {
        NotificacionDTO.BroadcastRequest request = NotificacionDTO.BroadcastRequest.builder()
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .build();
        crearBroadcast(request);
    }

    /**
     * Elimina notificaciones asociadas a una referencia.
     */
    @Transactional
    public int eliminarPorReferencia(String referenciaTipo, Long referenciaId) {
        int eliminadas = repository.eliminarPorReferencia(referenciaTipo, referenciaId);
        log.info("Eliminadas {} notificaciones de {} {}", eliminadas, referenciaTipo, referenciaId);
        return eliminadas;
    }

    // ==================== MAPPERS ====================

    private NotificacionDTO.ListResponse buildListResponse(Page<Notificacion> page, long noLeidas) {
        List<NotificacionDTO.Response> notificaciones = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return NotificacionDTO.ListResponse.builder()
                .notificaciones(notificaciones)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .noLeidas(noLeidas)
                .build();
    }

    private NotificacionDTO.Response mapToResponse(Notificacion n) {
        return NotificacionDTO.Response.builder()
                .id(n.getId())
                .usuarioId(n.getUsuario() != null ? n.getUsuario().getId() : null)
                .usuarioNombre(n.getUsuario() != null ? n.getUsuario().getNombre() : null)
                .tipo(n.getTipo())
                .tipoNombre(n.getTipo().getNombre())
                .tipoIcono(n.getTipo().getIcono())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .leida(n.getLeida())
                .fechaLectura(n.getFechaLectura())
                .referenciaTipo(n.getReferenciaTipo())
                .referenciaId(n.getReferenciaId())
                .createdAt(n.getCreatedAt())
                .esBroadcast(n.esBroadcast())
                .build();
    }
}
