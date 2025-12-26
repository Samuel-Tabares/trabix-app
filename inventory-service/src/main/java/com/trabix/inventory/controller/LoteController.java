package com.trabix.inventory.controller;

import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
import com.trabix.inventory.dto.*;
import com.trabix.inventory.entity.Usuario;
import com.trabix.inventory.service.InventarioService;
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

/**
 * Controlador REST para gestión de lotes.
 */
@RestController
@RequestMapping("/lotes")
@RequiredArgsConstructor
@Tag(name = "Lotes", description = "Gestión de lotes de granizados")
@SecurityRequirement(name = "bearerAuth")
public class LoteController {

    private final InventarioService inventarioService;

    @PostMapping
    @Operation(summary = "Crear lote", description = "Crea un nuevo lote para un vendedor. Solo ADMIN.")
    public ResponseEntity<ApiResponse<LoteResponse>> crearLote(
            @Valid @RequestBody CrearLoteRequest request) {
        
        LoteResponse response = inventarioService.crearLote(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Lote creado exitosamente. Tanda 1 liberada."));
    }

    @GetMapping
    @Operation(summary = "Listar lotes", description = "Lista todos los lotes con paginación. Solo ADMIN.")
    public ResponseEntity<ApiResponse<PaginaResponse<LoteResponse>>> listarLotes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaCreacion").descending());
        Page<LoteResponse> page = inventarioService.listarLotes(pageable);
        
        PaginaResponse<LoteResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener lote", description = "Obtiene un lote por ID.")
    public ResponseEntity<ApiResponse<LoteResponse>> obtenerLote(@PathVariable Long id) {
        LoteResponse response = inventarioService.obtenerLote(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Lotes de usuario", description = "Lista los lotes de un usuario.")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> listarLotesDeUsuario(
            @PathVariable Long usuarioId) {
        
        List<LoteResponse> response = inventarioService.listarLotesDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/usuario/{usuarioId}/activo")
    @Operation(summary = "Lote activo", description = "Obtiene el lote activo más antiguo de un usuario (FIFO).")
    public ResponseEntity<ApiResponse<LoteResponse>> obtenerLoteActivo(
            @PathVariable Long usuarioId) {
        
        LoteResponse response = inventarioService.obtenerLoteActivoDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/usuario/{usuarioId}/activos")
    @Operation(summary = "Lotes activos", description = "Obtiene todos los lotes activos de un usuario.")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> obtenerLotesActivos(
            @PathVariable Long usuarioId) {
        
        List<LoteResponse> response = inventarioService.obtenerLotesActivosDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Mi lote activo", description = "Obtiene el lote activo del usuario autenticado.")
    public ResponseEntity<ApiResponse<LoteResponse>> miLoteActivo(
            @AuthenticationPrincipal Usuario usuario) {
        
        LoteResponse response = inventarioService.obtenerLoteActivoDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/lotes")
    @Operation(summary = "Mis lotes", description = "Lista todos los lotes del usuario autenticado.")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> misLotes(
            @AuthenticationPrincipal Usuario usuario) {
        
        List<LoteResponse> response = inventarioService.listarLotesDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/activos")
    @Operation(summary = "Mis lotes activos", description = "Lista los lotes activos del usuario autenticado (FIFO).")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> misLotesActivos(
            @AuthenticationPrincipal Usuario usuario) {
        
        List<LoteResponse> response = inventarioService.obtenerLotesActivosDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/stock")
    @Operation(summary = "Mi stock", description = "Obtiene el stock actual del usuario autenticado.")
    public ResponseEntity<ApiResponse<Integer>> miStock(
            @AuthenticationPrincipal Usuario usuario) {
        
        int stock = inventarioService.obtenerStockActualUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar lote", description = "Cancela un lote sin ventas. Solo ADMIN.")
    public ResponseEntity<ApiResponse<Void>> cancelarLote(@PathVariable Long id) {
        inventarioService.cancelarLote(id);
        return ResponseEntity.ok(ApiResponse.ok("Lote cancelado"));
    }
}
