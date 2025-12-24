package com.trabix.finance.controller;

import com.trabix.finance.dto.CostoProduccionDTO;
import com.trabix.finance.service.CostoProduccionService;
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
 * Solo admin puede acceder.
 * Tipos: PRODUCCION, INSUMO, MARKETING, OTRO
 */
@RestController
@RequestMapping("/costos/produccion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CostoProduccionController {

    private final CostoProduccionService service;

    @PostMapping
    public ResponseEntity<CostoProduccionDTO.Response> crear(
            @Valid @RequestBody CostoProduccionDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostoProduccionDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostoProduccionDTO.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CostoProduccionDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CostoProduccionDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<CostoProduccionDTO.ListResponse> listarPorTipo(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listarPorTipo(tipo.toUpperCase(), pageable));
    }

    @GetMapping("/periodo")
    public ResponseEntity<CostoProduccionDTO.ListResponse> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listarPorPeriodo(desde, hasta, pageable));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<CostoProduccionDTO.Response>> buscar(@RequestParam String concepto) {
        return ResponseEntity.ok(service.buscarPorConcepto(concepto));
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<CostoProduccionDTO.Response>> listarRecientes() {
        return ResponseEntity.ok(service.listarUltimos());
    }

    @GetMapping("/resumen")
    public ResponseEntity<CostoProduccionDTO.ResumenGeneral> obtenerResumen() {
        return ResponseEntity.ok(service.obtenerResumenGeneral());
    }

    @GetMapping("/resumen/periodo")
    public ResponseEntity<CostoProduccionDTO.ResumenGeneral> obtenerResumenPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(service.obtenerResumenPeriodo(desde, hasta));
    }
}
