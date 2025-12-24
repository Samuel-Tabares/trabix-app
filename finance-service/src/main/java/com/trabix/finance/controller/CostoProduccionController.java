package com.trabix.finance.controller;

import com.trabix.finance.dto.CostoProduccionDTO;
import com.trabix.finance.service.CostoProduccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador para costos de producción.
 * Solo admin puede crear, modificar y eliminar.
 * Tipos: PRODUCCION, INSUMO, MARKETING, OTRO
 */
@RestController
@RequestMapping("/costos/produccion")
@RequiredArgsConstructor
@Tag(name = "Costos de Producción", description = "Registro y gestión de gastos del negocio")
public class CostoProduccionController {

    private final CostoProduccionService service;

    // === CRUD ===

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar costo", description = "Solo admin - Crea nuevo registro de gasto")
    public ResponseEntity<CostoProduccionDTO.Response> crear(
            @Valid @RequestBody CostoProduccionDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener costo", description = "Solo admin - Detalle de un costo")
    public ResponseEntity<CostoProduccionDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar costo", description = "Solo admin - Modifica un costo existente")
    public ResponseEntity<CostoProduccionDTO.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CostoProduccionDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar costo", description = "Solo admin - Elimina un registro")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // === Listados ===

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar costos", description = "Solo admin - Lista paginada de costos")
    public ResponseEntity<CostoProduccionDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar por tipo", 
               description = "Solo admin - PRODUCCION, INSUMO, MARKETING, OTRO")
    public ResponseEntity<CostoProduccionDTO.ListResponse> listarPorTipo(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listarPorTipo(tipo.toUpperCase(), pageable));
    }

    @GetMapping("/periodo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar por período", description = "Solo admin - Costos en un rango de fechas")
    public ResponseEntity<CostoProduccionDTO.ListResponse> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listarPorPeriodo(desde, hasta, pageable));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar por concepto", description = "Solo admin - Búsqueda por texto")
    public ResponseEntity<List<CostoProduccionDTO.Response>> buscar(
            @RequestParam String concepto) {
        return ResponseEntity.ok(service.buscarPorConcepto(concepto));
    }

    @GetMapping("/recientes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Últimos costos", description = "Solo admin - Los 10 costos más recientes")
    public ResponseEntity<List<CostoProduccionDTO.Response>> listarRecientes() {
        return ResponseEntity.ok(service.listarUltimos());
    }

    // === Reportes ===

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen general", description = "Solo admin - Totales por tipo de costo")
    public ResponseEntity<CostoProduccionDTO.ResumenGeneral> obtenerResumen() {
        return ResponseEntity.ok(service.obtenerResumenGeneral());
    }

    @GetMapping("/resumen/periodo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen por período", 
               description = "Solo admin - Totales por tipo en un rango de fechas")
    public ResponseEntity<CostoProduccionDTO.ResumenGeneral> obtenerResumenPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(service.obtenerResumenPeriodo(desde, hasta));
    }
}
