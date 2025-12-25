package com.trabix.backup.controller;

import com.trabix.backup.dto.BackupDTO;
import com.trabix.backup.entity.Usuario;
import com.trabix.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de backups.
 * SOLO ADMIN puede acceder a estos endpoints.
 */
@RestController
@RequestMapping("/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService service;

    // === Crear backup ===

    @PostMapping
    public ResponseEntity<BackupDTO.Response> crearBackup(
            @RequestBody(required = false) BackupDTO.CreateRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crearBackup(request, usuario.getId()));
    }

    // === Consultas ===

    @GetMapping("/{id}")
    public ResponseEntity<BackupDTO.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping
    public ResponseEntity<BackupDTO.ListResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/completados")
    public ResponseEntity<BackupDTO.ListResponse> listarCompletados(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending());
        return ResponseEntity.ok(service.listarCompletados(pageable));
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<BackupDTO.Response>> listarRecientes() {
        return ResponseEntity.ok(service.listarRecientes());
    }

    @GetMapping("/resumen")
    public ResponseEntity<BackupDTO.ResumenResponse> obtenerResumen() {
        return ResponseEntity.ok(service.obtenerResumen());
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<BackupDTO.EstadisticasBackup> obtenerEstadisticasActuales() {
        return ResponseEntity.ok(service.obtenerEstadisticasActuales());
    }

    // === Descargar backup ===

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        Resource resource = service.descargarBackup(id);
        BackupDTO.Response backup = service.obtener(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + backup.getNombre() + ".zip\"")
                .body(resource);
    }

    // === Eliminar backup ===

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Backup eliminado correctamente"));
    }
}
