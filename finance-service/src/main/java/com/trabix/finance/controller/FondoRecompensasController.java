package com.trabix.finance.controller;

import com.trabix.finance.dto.FondoRecompensasDTO;
import com.trabix.finance.dto.MovimientoFondoDTO;
import com.trabix.finance.service.FondoRecompensasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador para el Fondo de Recompensas.
 * 
 * El fondo se alimenta SOLO cuando VENDEDORES pagan lotes.
 * El dinero del ADMIN/dueño NUNCA va al fondo.
 * $200 por TRABIX (configurable).
 * 
 * Consultas: todos los autenticados
 * Operaciones (ingresar, retirar, premiar): solo ADMIN
 */
@RestController
@RequestMapping("/fondo")
@RequiredArgsConstructor
public class FondoRecompensasController {

    private final FondoRecompensasService service;

    // ==================== CONSULTAS (todos los autenticados) ====================

    @GetMapping("/saldo")
    public ResponseEntity<FondoRecompensasDTO.SaldoResponse> obtenerSaldo() {
        return ResponseEntity.ok(service.obtenerSaldo());
    }

    @GetMapping("/movimientos")
    public ResponseEntity<MovimientoFondoDTO.ListResponse> listarMovimientos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fecha").descending());
        return ResponseEntity.ok(service.listarMovimientos(pageable));
    }

    @GetMapping("/movimientos/recientes")
    public ResponseEntity<List<MovimientoFondoDTO.Response>> listarRecientes() {
        return ResponseEntity.ok(service.listarUltimosMovimientos());
    }

    @GetMapping("/resumen")
    public ResponseEntity<FondoRecompensasDTO.ResumenPeriodo> obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(service.obtenerResumenPeriodo(desde, hasta));
    }

    @GetMapping("/premios/{usuarioId}")
    public ResponseEntity<MovimientoFondoDTO.ResumenBeneficiario> obtenerPremiosUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obtenerPremiosBeneficiario(usuarioId));
    }

    @GetMapping("/aportes/{vendedorId}")
    public ResponseEntity<MovimientoFondoDTO.ResumenVendedor> obtenerAportesVendedor(
            @PathVariable Long vendedorId) {
        return ResponseEntity.ok(service.obtenerAportesVendedor(vendedorId));
    }

    // ==================== OPERACIONES (solo ADMIN) ====================

    /**
     * Registra un ingreso al fondo.
     * Se usa cuando un VENDEDOR paga un lote.
     * 
     * Si se proporciona vendedorId, se valida que sea vendedor (no admin).
     * El dinero del ADMIN nunca va al fondo.
     */
    @PostMapping("/ingresar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoFondoDTO.Response> ingresar(
            @Valid @RequestBody FondoRecompensasDTO.IngresoRequest request) {
        return ResponseEntity.ok(service.ingresar(request));
    }

    /**
     * Retira dinero del fondo (sin beneficiario específico).
     */
    @PostMapping("/retirar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoFondoDTO.Response> retirar(
            @Valid @RequestBody FondoRecompensasDTO.RetiroRequest request) {
        return ResponseEntity.ok(service.retirar(request));
    }

    /**
     * Entrega un premio a un beneficiario.
     */
    @PostMapping("/premiar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoFondoDTO.Response> premiar(
            @Valid @RequestBody FondoRecompensasDTO.PremioRequest request) {
        return ResponseEntity.ok(service.premiar(request));
    }
}
