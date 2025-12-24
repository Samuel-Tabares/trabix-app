package com.trabix.finance.controller;

import com.trabix.finance.dto.ConfiguracionCostosDTO;
import com.trabix.finance.service.ConfiguracionCostosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para configuración de costos.
 * Solo admin puede ver costos reales y modificar.
 */
@RestController
@RequestMapping("/costos/configuracion")
@RequiredArgsConstructor
public class ConfiguracionCostosController {

    private final ConfiguracionCostosService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionCostosDTO.Response> obtener() {
        return ResponseEntity.ok(service.obtenerConfiguracion());
    }

    @GetMapping("/vendedor")
    public ResponseEntity<ConfiguracionCostosDTO.VendedorView> obtenerVistaVendedor() {
        return ResponseEntity.ok(service.obtenerVistaVendedor());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionCostosDTO.Response> actualizar(
            @Valid @RequestBody ConfiguracionCostosDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(request));
    }
}
