package com.trabix.billing.controller;

import com.trabix.billing.dto.*;
import com.trabix.billing.entity.Usuario;
import com.trabix.billing.service.CuadreService;
import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
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
public class CuadreController {

    private final CuadreService cuadreService;

    // === Endpoints para ADMIN ===

    @PostMapping("/generar")
    public ResponseEntity<ApiResponse<CuadreResponse>> generarCuadre(
            @Valid @RequestBody GenerarCuadreRequest request) {
        boolean forzar = request.getForzar() != null && request.getForzar();
        CuadreResponse response = cuadreService.generarCuadre(request.getTandaId(), forzar);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Cuadre generado. Esperando transferencia."));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ApiResponse<CuadreResponse>> confirmarCuadre(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarCuadreRequest request) {
        CuadreResponse response = cuadreService.confirmarCuadre(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cuadre confirmado exitosamente."));
    }

    @GetMapping("/pendientes")
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
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarPendientes() {
        ResumenCuadresResponse resumen = cuadreService.obtenerResumen();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "pendientes", (long) resumen.getCuadresPendientes(),
                "enProceso", (long) resumen.getCuadresEnProceso()
        )));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenCuadresResponse>> obtenerResumen() {
        ResumenCuadresResponse response = cuadreService.obtenerResumen();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/detectar")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> detectarTandasParaCuadre() {
        List<CuadreResponse> response = cuadreService.detectarTandasParaCuadre();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints de consulta ===

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CuadreResponse>> obtenerCuadre(@PathVariable Long id) {
        CuadreResponse response = cuadreService.obtenerCuadre(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/whatsapp")
    public ResponseEntity<ApiResponse<Map<String, String>>> obtenerTextoWhatsApp(@PathVariable Long id) {
        String texto = cuadreService.obtenerTextoWhatsApp(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("texto", texto)));
    }

    @GetMapping("/{id}/calculo")
    public ResponseEntity<ApiResponse<CalculoCuadreResponse>> obtenerDetalleCalculo(@PathVariable Long id) {
        CalculoCuadreResponse response = cuadreService.obtenerDetalleCalculo(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints por lote ===

    @GetMapping("/lote/{loteId}")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> cuadresDeLote(@PathVariable Long loteId) {
        List<CuadreResponse> response = cuadreService.listarCuadresDeLote(loteId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Endpoints por usuario ===

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> cuadresDeUsuario(@PathVariable Long usuarioId) {
        List<CuadreResponse> response = cuadreService.listarCuadresDeUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CuadreResponse>>> misCuadres(
            @AuthenticationPrincipal Usuario usuario) {
        List<CuadreResponse> response = cuadreService.listarCuadresDeUsuario(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
