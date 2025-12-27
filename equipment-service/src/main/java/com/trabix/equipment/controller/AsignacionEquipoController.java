package com.trabix.equipment.controller;

import com.trabix.equipment.dto.AsignacionEquipoDTO;
import com.trabix.equipment.entity.EstadoAsignacion;
import com.trabix.equipment.entity.Usuario;
import com.trabix.equipment.service.AsignacionEquipoService;
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

/**
 * Controlador para gestión de asignaciones de equipos (kits nevera + pijama).
 */
@RestController
@RequestMapping("/asignaciones")
@RequiredArgsConstructor
public class AsignacionEquipoController {

    private final AsignacionEquipoService service;

    // ==================== OPERACIONES ADMIN ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> asignar(
            @Valid @RequestBody AsignacionEquipoDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionEquipoDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(service.devolver(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionEquipoDTO.CancelarRequest request) {
        return ResponseEntity.ok(service.cancelar(id, request));
    }

    @PostMapping("/{id}/confirmar-reposicion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> confirmarReposicion(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmarReposicion(id));
    }

    // ==================== CONSULTAS ADMIN ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.ListResponse> listarPorEstado(
            @PathVariable EstadoAsignacion estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listarPorEstado(estado, pageable));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AsignacionEquipoDTO.Response>> listarPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.ResumenUsuario> obtenerResumenUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obtenerResumenUsuario(usuarioId));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionEquipoDTO.ResumenGeneral> obtenerResumenGeneral() {
        return ResponseEntity.ok(service.obtenerResumenGeneral());
    }

    @GetMapping("/pendientes-reposicion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AsignacionEquipoDTO.Response>> listarPendientesReposicion() {
        return ResponseEntity.ok(service.listarCanceladasPendientesReposicion());
    }

    // ==================== CONSULTAS VENDEDOR AUTENTICADO ====================

    @GetMapping("/me")
    public ResponseEntity<List<AsignacionEquipoDTO.Response>> misAsignaciones(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/me/activa")
    public ResponseEntity<AsignacionEquipoDTO.Response> miAsignacionActiva(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.obtenerAsignacionActiva(usuario.getId()));
    }

    @GetMapping("/me/resumen")
    public ResponseEntity<AsignacionEquipoDTO.ResumenUsuario> miResumen(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.obtenerResumenUsuario(usuario.getId()));
    }

    @GetMapping("/me/bloqueado")
    public ResponseEntity<Boolean> estoyBloqueado(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.tienePagosPendientes(usuario.getId()));
    }
}
