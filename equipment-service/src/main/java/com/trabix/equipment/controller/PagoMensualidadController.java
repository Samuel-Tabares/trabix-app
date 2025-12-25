package com.trabix.equipment.controller;

import com.trabix.equipment.dto.PagoMensualidadDTO;
import com.trabix.equipment.entity.Usuario;
import com.trabix.equipment.service.PagoMensualidadService;
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

    // === Operaciones Admin ===

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
            @RequestParam Integer mes,
            @RequestParam Integer anio) {
        int generadas = service.generarMensualidades(mes, anio);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Mensualidades generadas exitosamente",
                "mes", mes,
                "anio", anio,
                "generadas", generadas
        ));
    }

    // === Consultas Admin ===

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
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("anio", "mes").ascending());
        return ResponseEntity.ok(service.listarPendientes(pageable));
    }

    @GetMapping("/pagados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoMensualidadDTO.ListResponse> listarPagados(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaPago").descending());
        return ResponseEntity.ok(service.listarPagados(pageable));
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarPorEquipo(
            @PathVariable Long equipoId) {
        return ResponseEntity.ok(service.listarPorEquipo(equipoId));
    }

    @GetMapping("/usuario/{usuarioId}/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> listarPendientesPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPendientesPorUsuario(usuarioId));
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

    // === Consultas del vendedor autenticado ===

    @GetMapping("/me/pendientes")
    public ResponseEntity<List<PagoMensualidadDTO.Response>> misPagosPendientes(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.listarPendientesPorUsuario(usuario.getId()));
    }
}
