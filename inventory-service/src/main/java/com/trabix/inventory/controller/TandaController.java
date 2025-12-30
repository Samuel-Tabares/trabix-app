package com.trabix.inventory.controller;

import com.trabix.common.dto.ApiResponse;
import com.trabix.inventory.dto.TandaResponse;
import com.trabix.inventory.entity.Usuario;
import com.trabix.inventory.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de tandas.
 */
@RestController
@RequestMapping("/tandas")
@RequiredArgsConstructor
@Tag(name = "Tandas", description = "Gestión de tandas de un lote")
@SecurityRequirement(name = "bearerAuth")
public class TandaController {

    private final InventarioService inventarioService;

    @GetMapping("/lote/{loteId}")
    @Operation(summary = "Tandas de lote", description = "Lista las tandas de un lote.")
    public ResponseEntity<ApiResponse<List<TandaResponse>>> tandasDeLote(
            @PathVariable Long loteId) {
        
        List<TandaResponse> response = inventarioService.obtenerTandasDeLote(loteId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tanda", description = "Obtiene una tanda por ID.")
    public ResponseEntity<ApiResponse<TandaResponse>> obtenerTanda(@PathVariable Long id) {
        TandaResponse response = inventarioService.obtenerTanda(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/pendientes-cuadre")
    @Operation(summary = "Tandas para cuadre", description = "Lista tandas con stock <= umbral%. Solo ADMIN.")
    public ResponseEntity<ApiResponse<List<TandaResponse>>> tandasParaCuadre() {
        List<TandaResponse> response = inventarioService.listarTandasParaCuadre();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/liberar")
    @Operation(summary = "Liberar tanda", description = "Libera la siguiente tanda de un lote. Solo ADMIN.")
    public ResponseEntity<ApiResponse<TandaResponse>> liberarTanda(@PathVariable Long id) {
        // id es el lote, no la tanda
        TandaResponse response = inventarioService.liberarSiguienteTanda(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Tanda liberada exitosamente"));
    }

    @PostMapping("/{id}/iniciar-cuadre")
    @Operation(summary = "Iniciar cuadre", description = "Marca una tanda como en proceso de cuadre. Solo ADMIN.")
    public ResponseEntity<ApiResponse<TandaResponse>> iniciarCuadre(@PathVariable Long id) {
        TandaResponse response = inventarioService.iniciarCuadreTanda(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cuadre iniciado"));
    }

    @PostMapping("/{id}/completar-cuadre")
    @Operation(summary = "Completar cuadre", description = "Marca una tanda como cuadrada. Solo ADMIN.")
    public ResponseEntity<ApiResponse<TandaResponse>> completarCuadre(@PathVariable Long id) {
        TandaResponse response = inventarioService.completarCuadreTanda(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cuadre completado"));
    }

    @PostMapping("/reducir-stock")
    @Operation(summary = "Reducir stock", description = "Reduce el stock de la tanda activa (por venta).")
    public ResponseEntity<ApiResponse<TandaResponse>> reducirStock(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam int cantidad) {
        
        TandaResponse response = inventarioService.reducirStock(usuario.getId(), cantidad);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
