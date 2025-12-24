package com.trabix.billing.controller;

import com.trabix.billing.dto.*;
import com.trabix.billing.entity.Usuario;
import com.trabix.billing.service.CuadreService;
import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
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
 * Controlador REST para gestión de cuadres.
 */
@RestController
@RequestMapping("/cuadres")
@RequiredArgsConstructor
@Tag(name = "Cuadres", description = "Gestión de cuadres de inversión y ganancias")
@SecurityRequirement(name = "bearerAuth")
public class CuadreController {

    private final CuadreService cuadreService;

    // === Endpoints para ADMIN ===

    @PostMapping("/generar")
    @Operation(summary = "Generar cuadre", description = "Genera un cuadre para una tanda. Solo ADMIN.")
    public ResponseEntity<ApiResponse<CuadreResponse>> generarCuadre(
            @Valid @RequestBody GenerarCuadreRequest request) {

        boolean forzar = request.getForzar() != null && request.getForzar();
        CuadreResponse response = cuadreService.generarCuadre(request.getTandaId(), forzar);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Cuadre generado. Esperando transferencia."));
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar cuadre", description = "Confirma que se recibió la transferencia. Solo ADMIN.")
    public ResponseEntity<ApiResponse<CuadreResponse>> confirmarCuadre(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarCuadreRequest request) {

        CuadreResponse response = cuadreService.confirmarCuadre(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cuadre confirmado exitosamente."));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Cuadres pendientes", description = "Lista cuadres pendientes de confirmación. Solo ADMIN.")
    public ResponseEntity<ApiResponse<PaginaResponse<CuadreResponse>>> listarPendientes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("createdAt").ascending());
        Page<CuadreResponse> page = cuadreService.listarCuadresPendientes(pageable);

        PaginaResponse<CuadreResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/pendientes/count")
    @Operation(summary = "Contar pendientes", description = "Cuenta cuadres pendientes.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarPendientes() {
        ResumenCuadresResponse resumen = cuadreService.obtenerResumen();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "pendientes", (long) resumen.getCuadresPendientes(),
                "enProceso", (long) resumen.getCuadresEnProceso()
        )));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de cuadres", description = "Obtiene resumen para el panel admin.")
    public ResponseEntity<ApiResponse<ResumenCuadresResponse>> obtenerResumen() {
        ResumenCuadresResponse response = cuadreService.obtenerResumen();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/detectar")
    @Operation(summary = "Detectar para cuadre", description = "Detecta tandas que requieren cuadre (stock <= 20%). Solo ADMIN.")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> detectarTandasParaCuadre() {
        List<CuadreResponse> response = cuadreService.detectarTandasParaCuadre();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints de consulta ===

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cuadre", description = "Obtiene un cuadre por ID.")
    public ResponseEntity<ApiResponse<CuadreResponse>> obtenerCuadre(@PathVariable Long id) {
        CuadreResponse response = cuadreService.obtenerCuadre(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/whatsapp")
    @Operation(summary = "Texto WhatsApp", description = "Obtiene el texto formateado para WhatsApp.")
    public ResponseEntity<ApiResponse<Map<String, String>>> obtenerTextoWhatsApp(@PathVariable Long id) {
        String texto = cuadreService.obtenerTextoWhatsApp(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("texto", texto)));
    }

    @GetMapping("/{id}/calculo")
    @Operation(summary = "Detalle del cálculo", description = "Obtiene el detalle paso a paso del cálculo.")
    public ResponseEntity<ApiResponse<CalculoCuadreResponse>> obtenerDetalleCalculo(
            @PathVariable Long id) {
        // id aquí es el ID de la tanda
        CalculoCuadreResponse response = cuadreService.obtenerDetalleCalculo(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints por lote ===

    @GetMapping("/lote/{loteId}")
    @Operation(summary = "Cuadres de lote", description = "Lista los cuadres de un lote específico.")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> cuadresDeLote(
            @PathVariable Long loteId) {

        List<CuadreResponse> response = cuadreService.listarCuadresDeLote(loteId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints por usuario ===

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Cuadres de usuario", description = "Lista los cuadres de un usuario.")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> cuadresDeUsuario(
            @PathVariable Long usuarioId) {

        List<CuadreResponse> response = cuadreService.listarCuadresDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Mis cuadres", description = "Lista los cuadres del usuario autenticado.")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> misCuadres(
            @AuthenticationPrincipal Usuario usuario) {

        List<CuadreResponse> response = cuadreService.listarCuadresDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
