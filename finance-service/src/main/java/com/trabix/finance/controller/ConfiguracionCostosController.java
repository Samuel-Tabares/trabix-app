package com.trabix.finance.controller;

import com.trabix.finance.dto.ConfiguracionCostosDTO;
import com.trabix.finance.service.ConfiguracionCostosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para configuración de costos.
 * Solo el admin puede ver los costos reales y modificar la configuración.
 * Los vendedores solo ven el costo percibido.
 */
@RestController
@RequestMapping("/costos/configuracion")
@RequiredArgsConstructor
@Tag(name = "Configuración de Costos", description = "Gestión de costos variables del sistema")
public class ConfiguracionCostosController {

    private final ConfiguracionCostosService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener configuración completa", 
               description = "Solo admin - Incluye costos reales y márgenes")
    public ResponseEntity<ConfiguracionCostosDTO.Response> obtener() {
        return ResponseEntity.ok(service.obtenerConfiguracion());
    }

    @GetMapping("/vendedor")
    @Operation(summary = "Obtener vista vendedor", 
               description = "Solo muestra el costo percibido ($2,400)")
    public ResponseEntity<ConfiguracionCostosDTO.VendedorView> obtenerVistaVendedor() {
        return ResponseEntity.ok(service.obtenerVistaVendedor());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar configuración", 
               description = "Solo admin - Modifica costos reales y aportes")
    public ResponseEntity<ConfiguracionCostosDTO.Response> actualizar(
            @Valid @RequestBody ConfiguracionCostosDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(request));
    }
}
