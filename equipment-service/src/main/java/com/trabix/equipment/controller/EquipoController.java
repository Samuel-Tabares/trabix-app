package com.trabix.equipment.controller;

import com.trabix.equipment.dto.EquipoDTO;
import com.trabix.equipment.entity.Usuario;
import com.trabix.equipment.service.EquipoService;
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
 * Controlador para gestión de equipos.
 */
@RestController
@RequestMapping("/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService service;

    // === Operaciones Admin ===

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.Response> asignar(
            @Valid @RequestBody EquipoDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipoDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.Response> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(service.devolver(id));
    }

    @PostMapping("/{id}/perdido")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.Response> marcarPerdido(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarPerdido(id));
    }

    // === Consultas Admin ===

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.ListResponse> listarPorEstado(
            @PathVariable String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listarPorEstado(estado, pageable));
    }

    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.ListResponse> listarPorTipo(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listarPorTipo(tipo, pageable));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EquipoDTO.Response>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.ResumenUsuario> obtenerResumenUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obtenerResumenUsuario(usuarioId));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipoDTO.ResumenGeneral> obtenerResumenGeneral() {
        return ResponseEntity.ok(service.obtenerResumenGeneral());
    }

    // === Consultas del vendedor autenticado ===

    @GetMapping("/me")
    public ResponseEntity<List<EquipoDTO.Response>> misEquipos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/me/activos")
    public ResponseEntity<List<EquipoDTO.Response>> misEquiposActivos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarEquiposActivosUsuario(usuario.getId()));
    }

    @GetMapping("/me/resumen")
    public ResponseEntity<EquipoDTO.ResumenUsuario> miResumen(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.obtenerResumenUsuario(usuario.getId()));
    }
}
