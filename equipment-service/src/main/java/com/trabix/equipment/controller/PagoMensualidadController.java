package com.trabix.equipment.controller;

import com.trabix.equipment.dto.PagoMensualidadDTO;
import com.trabix.equipment.entity.Usuario;
import com.trabix.equipment.service.PagoMensualidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para pagos de mensualidad.
 */
@RestController
@RequestMapping("/mensualidades")
@RequiredArgsConstructor
public class PagoMensualidadController {

    private final PagoMensualidadService service;

    // ==================== OPERACIONES ADMIN ====================

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.Response> registrarPago(
            @PathVariable Long id,
            @RequestBody(required = false) PagoMensualidadDTO.RegistrarPagoRequest request) {
        return ResponseEntity.ok(service.registrarPago(id, request));
    }

    @PostMapping("/generar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generarMensualidades(
            @Valid @RequestBody PagoMensualidadDTO.GenerarMensualidadesRequest request) {
        int generadas = service.generarMensualidades(request.getMes(), request.getAnio());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Mensualidades generadas exitosamente",
                "mes", request.getMes(),
                "anio", request.getAnio(),
                "generadas", generadas
        ));
    }

    // ==================== CONSULTAS ADMIN ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.ListResponse> listarPendientes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaVencimiento").ascending());
        return ResponseEntity.ok(service.listarPendientes(pageable));
    }

    @GetMapping("/vencidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.ListResponse> listarVencidos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaVencimiento").ascending());
        return ResponseEntity.ok(service.listarVencidos(pageable));
    }

    @GetMapping("/pagados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.ListResponse> listarPagados(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaPago").descending());
        return ResponseEntity.ok(service.listarPagados(pageable));
    }

    @GetMapping("/asignacion/{asignacionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarPorAsignacion(
            @PathVariable Long asignacionId) {
        return ResponseEntity.ok(service.listarPorAsignacion(asignacionId));
    }

    @GetMapping("/usuario/{usuarioId}/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarPendientesPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPendientesPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/vencidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarVencidosPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarVencidosPorUsuario(usuarioId));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarPorMes(
            @RequestParam Integer mes,
            @RequestParam Integer anio) {
        return ResponseEntity.ok(service.listarPorMes(mes, anio));
    }

    @GetMapping("/mes/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.ResumenMes> obtenerResumenMes(
            @RequestParam Integer mes,
            @RequestParam Integer anio) {
        return ResponseEntity.ok(service.obtenerResumenMes(mes, anio));
    }

    // ==================== CONSULTAS VENDEDOR AUTENTICADO ====================

    @GetMapping("/me/pendientes")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> misPagosPendientes(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarPendientesPorUsuario(usuario.getId()));
    }

    @GetMapping("/me/vencidos")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> misPagosVencidos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarVencidosPorUsuario(usuario.getId()));
    }
}
