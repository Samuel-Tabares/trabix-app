package com.trabix.inventory.controller;

import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
import com.trabix.inventory.dto.*;
import com.trabix.inventory.service.StockProduccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión del stock de producción de Samuel (N1).
 * Solo accesible por ADMIN.
 */
@RestController
@RequestMapping("/stock-produccion")
@RequiredArgsConstructor
@Tag(name = "Stock Producción", description = "Gestión del stock de producción de Samuel")
@SecurityRequirement(name = "bearerAuth")
public class StockProduccionController {

    private final StockProduccionService stockProduccionService;

    @GetMapping
    @Operation(summary = "Estado del stock", 
            description = "Obtiene el estado completo del stock de producción incluyendo reservados y déficit.")
    public ResponseEntity<ApiResponse<StockProduccionResponse>> obtenerEstadoStock() {
        StockProduccionResponse response = stockProduccionService.obtenerEstadoStock();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/produccion")
    @Operation(summary = "Registrar producción", 
            description = "Registra nueva producción de TRABIX.")
    public ResponseEntity<ApiResponse<StockProduccionResponse>> registrarProduccion(
            @Valid @RequestBody RegistrarProduccionRequest request) {
        
        StockProduccionResponse response = stockProduccionService.registrarProduccion(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Producción registrada: " + request.getCantidad() + " TRABIX"));
    }

    @PostMapping("/venta-directa")
    @Operation(summary = "Registrar venta directa", 
            description = "Registra venta directa de Samuel en eventos.")
    public ResponseEntity<ApiResponse<StockProduccionResponse>> registrarVentaDirecta(
            @RequestParam int cantidad,
            @RequestParam(required = false) String descripcion) {
        
        StockProduccionResponse response = stockProduccionService.registrarVentaDirecta(cantidad, descripcion);
        return ResponseEntity.ok(ApiResponse.ok(response, "Venta directa registrada: " + cantidad + " TRABIX"));
    }

    @PostMapping("/ajuste")
    @Operation(summary = "Ajustar stock", 
            description = "Ajuste manual de stock (positivo para agregar, negativo para reducir).")
    public ResponseEntity<ApiResponse<StockProduccionResponse>> ajustarStock(
            @RequestParam int cantidad,
            @RequestParam String motivo) {
        
        StockProduccionResponse response = stockProduccionService.ajustarStock(cantidad, motivo);
        String mensaje = cantidad >= 0 
                ? "Stock aumentado en " + cantidad + " TRABIX"
                : "Stock reducido en " + Math.abs(cantidad) + " TRABIX";
        return ResponseEntity.ok(ApiResponse.ok(response, mensaje));
    }

    @PutMapping("/alerta-stock-bajo")
    @Operation(summary = "Configurar alerta", 
            description = "Configura el nivel de alerta de stock bajo.")
    public ResponseEntity<ApiResponse<StockProduccionResponse>> configurarAlerta(
            @RequestParam int nivel) {
        
        StockProduccionResponse response = stockProduccionService.configurarAlertaStockBajo(nivel);
        return ResponseEntity.ok(ApiResponse.ok(response, "Nivel de alerta configurado: " + nivel + " TRABIX"));
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Historial de movimientos", 
            description = "Lista el historial de movimientos de stock con paginación.")
    public ResponseEntity<ApiResponse<PaginaResponse<MovimientoStockResponse>>> listarMovimientos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        
        Pageable pageable = PageRequest.of(pagina, tamanio);
        Page<MovimientoStockResponse> page = stockProduccionService.listarMovimientos(pageable);
        
        PaginaResponse<MovimientoStockResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/movimientos/recientes")
    @Operation(summary = "Últimos movimientos", 
            description = "Obtiene los últimos 10 movimientos de stock.")
    public ResponseEntity<ApiResponse<List<MovimientoStockResponse>>> obtenerUltimosMovimientos() {
        List<MovimientoStockResponse> response = stockProduccionService.obtenerUltimosMovimientos();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
