package com.trabix.sales.controller;

import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
import com.trabix.sales.dto.*;
import com.trabix.sales.entity.Usuario;
import com.trabix.sales.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de ventas.
 */
@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Registro y gestión de ventas")
@SecurityRequirement(name = "bearerAuth")
public class VentaController {

    private final VentaService ventaService;

    // === Endpoints para vendedores ===

    @PostMapping
    @Operation(summary = "Registrar venta", description = "Registra una nueva venta.")
    public ResponseEntity<ApiResponse<VentaResponse>> registrarVenta(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody RegistrarVentaRequest request) {

        VentaResponse response = ventaService.registrarVenta(usuario.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Venta registrada. Pendiente de aprobación."));
    }

    @GetMapping("/me")
    @Operation(summary = "Mis ventas", description = "Lista las ventas del usuario autenticado.")
    public ResponseEntity<ApiResponse<List<VentaResponse>>> misVentas(
            @AuthenticationPrincipal Usuario usuario) {

        List<VentaResponse> response = ventaService.listarVentasDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/hoy")
    @Operation(summary = "Mis ventas de hoy", description = "Lista las ventas del día.")
    public ResponseEntity<ApiResponse<List<VentaResponse>>> misVentasHoy(
            @AuthenticationPrincipal Usuario usuario) {

        List<VentaResponse> response = ventaService.listarVentasHoy(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/resumen")
    @Operation(summary = "Mi resumen", description = "Obtiene resumen de ventas del usuario.")
    public ResponseEntity<ApiResponse<ResumenVentasResponse>> miResumen(
            @AuthenticationPrincipal Usuario usuario) {

        ResumenVentasResponse response = ventaService.obtenerResumenUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/stock")
    @Operation(summary = "Mi stock", description = "Obtiene el stock actual del usuario.")
    public ResponseEntity<ApiResponse<Integer>> miStock(
            @AuthenticationPrincipal Usuario usuario) {

        int stock = ventaService.obtenerStockUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    // === Endpoints para admin ===

    @GetMapping
    @Operation(summary = "Listar ventas", description = "Lista todas las ventas. Solo ADMIN.")
    public ResponseEntity<ApiResponse<PaginaResponse<VentaResponse>>> listarVentas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaRegistro").descending());
        Page<VentaResponse> page = ventaService.listarVentas(pageable);

        PaginaResponse<VentaResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Ventas pendientes", description = "Lista ventas pendientes de aprobar. Solo ADMIN.")
    public ResponseEntity<ApiResponse<PaginaResponse<VentaResponse>>> listarPendientes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaRegistro").ascending());
        Page<VentaResponse> page = ventaService.listarVentasPendientes(pageable);

        PaginaResponse<VentaResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/pendientes/count")
    @Operation(summary = "Contar pendientes", description = "Cuenta ventas pendientes.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarPendientes() {
        long count = ventaService.contarVentasPendientes();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("pendientes", count)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta", description = "Obtiene una venta por ID.")
    public ResponseEntity<ApiResponse<VentaResponse>> obtenerVenta(@PathVariable Long id) {
        VentaResponse response = ventaService.obtenerVenta(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar venta", description = "Aprueba una venta pendiente. Solo ADMIN.")
    public ResponseEntity<ApiResponse<VentaResponse>> aprobarVenta(@PathVariable Long id) {
        VentaResponse response = ventaService.aprobarVenta(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Venta aprobada"));
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar venta", description = "Rechaza una venta pendiente. Solo ADMIN.")
    public ResponseEntity<ApiResponse<VentaResponse>> rechazarVenta(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Sin especificar") String motivo) {

        VentaResponse response = ventaService.rechazarVenta(id, motivo);
        return ResponseEntity.ok(ApiResponse.ok(response, "Venta rechazada. Stock restaurado."));
    }

    // === Consultas por usuario específico ===

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Ventas de usuario", description = "Lista ventas de un usuario específico.")
    public ResponseEntity<ApiResponse<List<VentaResponse>>> ventasDeUsuario(
            @PathVariable Long usuarioId) {

        List<VentaResponse> response = ventaService.listarVentasDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/usuario/{usuarioId}/resumen")
    @Operation(summary = "Resumen de usuario", description = "Obtiene resumen de ventas de un usuario.")
    public ResponseEntity<ApiResponse<ResumenVentasResponse>> resumenDeUsuario(
            @PathVariable Long usuarioId) {

        ResumenVentasResponse response = ventaService.obtenerResumenUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Consultas por tanda ===

    @GetMapping("/tanda/{tandaId}")
    @Operation(summary = "Ventas de tanda", description = "Lista ventas de una tanda específica.")
    public ResponseEntity<ApiResponse<List<VentaResponse>>> ventasDeTanda(
            @PathVariable Long tandaId) {

        List<VentaResponse> response = ventaService.listarVentasDeTanda(tandaId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
