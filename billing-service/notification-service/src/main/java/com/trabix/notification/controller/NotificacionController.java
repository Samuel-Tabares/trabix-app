package com.trabix.notification.controller;

import com.trabix.notification.dto.NotificacionDTO;
import com.trabix.notification.entity.Usuario;
import com.trabix.notification.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de notificaciones.
 */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService service;

    // === Operaciones Admin ===

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.Response> crear(
            @Valid @RequestBody NotificacionDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.Response> crearBroadcast(
            @Valid @RequestBody NotificacionDTO.BroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearBroadcast(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Notificación eliminada"));
    }

    @DeleteMapping("/limpiar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> limpiarAntiguas(
            @RequestParam(defaultValue = "30") int dias) {
        int eliminadas = service.limpiarAntiguas(dias);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Notificaciones antiguas eliminadas",
                "eliminadas", eliminadas,
                "diasAntiguedad", dias
        ));
    }

    // === Consultas Admin ===

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.ListResponse> listarTodas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listarTodas(pageable));
    }

    @GetMapping("/broadcasts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.ListResponse> listarBroadcasts(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listarBroadcasts(pageable));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.ListResponse> listarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId, pageable));
    }

    @GetMapping("/resumen/tipos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificacionDTO.ResumenTipos> obtenerResumenTipos() {
        return ResponseEntity.ok(service.obtenerResumenTipos());
    }

    // === Mis notificaciones (usuario autenticado) ===

    @GetMapping("/me")
    public ResponseEntity<NotificacionDTO.ListResponse> misNotificaciones(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listarPorUsuario(usuario.getId(), pageable));
    }

    @GetMapping("/me/no-leidas")
    public ResponseEntity<List<NotificacionDTO.Response>> misNoLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarNoLeidasPorUsuario(usuario.getId()));
    }

    @GetMapping("/me/recientes")
    public ResponseEntity<List<NotificacionDTO.Response>> misRecientes(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarRecientesPorUsuario(usuario.getId()));
    }

    @GetMapping("/me/contador")
    public ResponseEntity<NotificacionDTO.ContadorResponse> miContador(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.contarPorUsuario(usuario.getId()));
    }

    @PostMapping("/me/marcar-leida/{id}")
    public ResponseEntity<NotificacionDTO.Response> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarLeida(id));
    }

    @PostMapping("/me/marcar-todas-leidas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        int actualizadas = service.marcarTodasLeidas(usuario.getId());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Notificaciones marcadas como leídas",
                "actualizadas", actualizadas
        ));
    }

    @PostMapping("/me/marcar-leidas")
    public ResponseEntity<Map<String, Object>> marcarLeidasPorIds(
            @Valid @RequestBody NotificacionDTO.MarcarLeidasRequest request) {
        int actualizadas = service.marcarLeidasPorIds(request.getIds());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Notificaciones marcadas como leídas",
                "actualizadas", actualizadas
        ));
    }

    @DeleteMapping("/me/limpiar-leidas")
    public ResponseEntity<Map<String, Object>> eliminarMisLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        int eliminadas = service.eliminarLeidasDeUsuario(usuario.getId());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Notificaciones leídas eliminadas",
                "eliminadas", eliminadas
        ));
    }

    // === Obtener por ID ===

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }
}
