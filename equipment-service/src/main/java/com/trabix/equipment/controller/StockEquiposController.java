package com.trabix.equipment.controller;

import com.trabix.equipment.dto.StockEquiposDTO;
import com.trabix.equipment.service.StockEquiposService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestión del stock de equipos.
 * Solo accesible por ADMIN.
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StockEquiposController {

    private final StockEquiposService service;

    @GetMapping
    public ResponseEntity<StockEquiposDTO.Response> obtenerStock() {
        return ResponseEntity.ok(service.obtenerResumen());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<Integer> obtenerDisponibles() {
        return ResponseEntity.ok(service.obtenerDisponibles());
    }

    @PostMapping("/agregar")
    public ResponseEntity<StockEquiposDTO.Response> agregarKits(
            @Valid @RequestBody StockEquiposDTO.AgregarKitsRequest request) {
        return ResponseEntity.ok(service.agregarKits(request.getCantidad()));
    }

    @PutMapping("/ajustar")
    public ResponseEntity<StockEquiposDTO.Response> ajustarStock(
            @Valid @RequestBody StockEquiposDTO.AjustarStockRequest request) {
        return ResponseEntity.ok(service.ajustarStock(request.getNuevoValor()));
    }
}
