package com.trabix.document.controller;

import com.trabix.document.dto.DocumentoDTO;
import com.trabix.document.entity.Usuario;
import com.trabix.document.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestión de documentos.
 * Cotizaciones y Facturas.
 */
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService service;

    // === CRUD ===

    @PostMapping
    public ResponseEntity<DocumentoDTO.Response> crear(
            @Valid @RequestBody DocumentoDTO.CreateRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDTO.Response> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocumentoDTO.UpdateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<DocumentoDTO.Response> obtenerPorNumero(@PathVariable String numero) {
        return ResponseEntity.ok(service.obtenerPorNumero(numero));
    }

    // === Acciones ===

    @PostMapping("/{id}/emitir")
    public ResponseEntity<DocumentoDTO.Response> emitir(@PathVariable Long id) {
        return ResponseEntity.ok(service.emitir(id));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<DocumentoDTO.Response> marcarPagado(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarPagado(id));
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<DocumentoDTO.Response> anular(@PathVariable Long id) {
        return ResponseEntity.ok(service.anular(id));
    }

    @PostMapping("/{id}/convertir-factura")
    public ResponseEntity<DocumentoDTO.Response> convertirAFactura(
            @PathVariable Long id,
            @RequestBody(required = false) DocumentoDTO.ConvertirAFacturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.convertirAFactura(id, request));
    }

    // === Listados ===

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaEmision").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<DocumentoDTO.ListResponse> listarPorTipo(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaEmision").descending());
        return ResponseEntity.ok(service.listarPorTipo(tipo, pageable));
    }

    @GetMapping("/tipo/{tipo}/estado/{estado}")
    public ResponseEntity<DocumentoDTO.ListResponse> listarPorTipoYEstado(
            @PathVariable String tipo,
            @PathVariable String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaEmision").descending());
        return ResponseEntity.ok(service.listarPorTipoYEstado(tipo, estado, pageable));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoDTO.ListResponse> listarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaEmision").descending());
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId, pageable));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<DocumentoDTO.Response>> buscarPorCliente(
            @RequestParam String cliente) {
        return ResponseEntity.ok(service.buscarPorCliente(cliente));
    }

    @GetMapping("/recientes/{tipo}")
    public ResponseEntity<List<DocumentoDTO.Response>> listarRecientes(@PathVariable String tipo) {
        return ResponseEntity.ok(service.listarRecientes(tipo));
    }

    // === Mis documentos ===

    @GetMapping("/me")
    public ResponseEntity<DocumentoDTO.ListResponse> misDocumentos(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaEmision").descending());
        return ResponseEntity.ok(service.listarPorUsuario(usuario.getId(), pageable));
    }

    // === Resúmenes ===

    @GetMapping("/resumen/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoDTO.ResumenDocumentos> obtenerResumen(@PathVariable String tipo) {
        return ResponseEntity.ok(service.obtenerResumen(tipo));
    }
}
