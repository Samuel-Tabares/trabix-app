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
 * 
 * GET /costos/configuracion - Solo ADMIN, muestra todo incluyendo costo real
 * GET /costos/configuracion/vendedor - Autenticados, solo muestra costo percibido
 * PUT /costos/configuracion - Solo ADMIN, actualiza configuración
 */
@RestController
@RequestMapping("/costos/configuracion")
@RequiredArgsConstructor
public class ConfiguracionCostosController {

    private final ConfiguracionCostosService service;

    /**
     * Obtiene la configuración completa (solo ADMIN).
     * Incluye costo real, percibido, aporte fondo y diferencia.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionCostosDTO.Response> obtener() {
        return ResponseEntity.ok(service.obtenerConfiguracion());
    }

    /**
     * Obtiene vista para vendedores.
     * Solo muestra el costo percibido (lo que pagan por TRABIX).
     */
    @GetMapping("/vendedor")
    public ResponseEntity<ConfiguracionCostosDTO.VendedorView> obtenerVistaVendedor() {
        return ResponseEntity.ok(service.obtenerVistaVendedor());
    }

    /**
     * Actualiza la configuración de costos (solo ADMIN).
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionCostosDTO.Response> actualizar(
            @Valid @RequestBody ConfiguracionCostosDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(request));
    }
}
