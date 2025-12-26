package com.trabix.user.controller;

import com.trabix.common.dto.ApiResponse;
import com.trabix.common.dto.PaginaResponse;
import com.trabix.common.enums.RolUsuario;
import com.trabix.common.exception.AccesoNoAutorizadoException;
import com.trabix.user.dto.*;
import com.trabix.user.entity.Usuario;
import com.trabix.user.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de usuarios.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios y árbol de cascada")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un nuevo vendedor. Solo ADMIN.")
    public ResponseEntity<ApiResponse<UsuarioCreadoResponse>> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request) {
        
        UsuarioCreadoResponse response = usuarioService.crearUsuario(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Usuario creado exitosamente"));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Lista todos los usuarios con paginación. Solo ADMIN.")
    public ResponseEntity<ApiResponse<PaginaResponse<UsuarioResponse>>> listarUsuarios(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaIngreso").descending());
        Page<UsuarioResponse> page = usuarioService.listarUsuarios(pageable, soloActivos);
        
        PaginaResponse<UsuarioResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/vendedores")
    @Operation(summary = "Listar vendedores", description = "Lista vendedores activos (excluyendo admin).")
    public ResponseEntity<ApiResponse<PaginaResponse<UsuarioResponse>>> listarVendedores(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("nombre").ascending());
        Page<UsuarioResponse> page = usuarioService.listarVendedores(pageable);
        
        PaginaResponse<UsuarioResponse> response = PaginaResponse.of(
                page.getContent(), pagina, tamanio, page.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario", description = "Obtiene un usuario por ID.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuario(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        // Verificar permiso: admin puede ver todos, otros solo a sí mismos o sus reclutados
        if (usuarioActual.getRol() != RolUsuario.ADMIN && !usuarioActual.getId().equals(id)) {
            // Verificar si es su reclutado
            if (!esReclutadoDe(id, usuarioActual.getId())) {
                throw new AccesoNoAutorizadoException("No tiene permiso para ver este usuario");
            }
        }
        
        UsuarioResponse response = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/cedula/{cedula}")
    @Operation(summary = "Buscar por cédula", description = "Busca un usuario por su cédula.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPorCedula(
            @PathVariable String cedula,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        UsuarioResponse response = usuarioService.obtenerUsuarioPorCedula(cedula);
        
        // Verificar permiso
        if (usuarioActual.getRol() != RolUsuario.ADMIN && !usuarioActual.getCedula().equals(cedula)) {
            if (!esReclutadoDe(response.getId(), usuarioActual.getId())) {
                throw new AccesoNoAutorizadoException("No tiene permiso para ver este usuario");
            }
        }
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar por nombre", description = "Busca usuarios por nombre.")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarPorNombre(
            @RequestParam String nombre) {
        
        List<UsuarioResponse> response = usuarioService.buscarPorNombre(nombre);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza datos de un usuario.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        // Solo admin o el propio usuario pueden actualizar
        if (usuarioActual.getRol() != RolUsuario.ADMIN && !usuarioActual.getId().equals(id)) {
            throw new AccesoNoAutorizadoException("No tiene permiso para actualizar este usuario");
        }
        
        UsuarioResponse response = usuarioService.actualizarUsuario(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Usuario actualizado"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar usuario", description = "Desactiva un usuario (soft delete). Solo ADMIN.")
    public ResponseEntity<ApiResponse<Void>> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado"));
    }

    @PostMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar usuario", description = "Reactiva un usuario desactivado. Solo ADMIN.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> reactivarUsuario(@PathVariable Long id) {
        UsuarioResponse response = usuarioService.reactivarUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Usuario reactivado"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Resetear contraseña", description = "Genera nueva contraseña. Solo ADMIN.")
    public ResponseEntity<ApiResponse<String>> resetearPassword(@PathVariable Long id) {
        String nuevoPassword = usuarioService.resetearPassword(id);
        return ResponseEntity.ok(ApiResponse.ok(nuevoPassword, "Contraseña reseteada. Nueva contraseña generada."));
    }

    // === Endpoints del árbol de cascada ===

    @GetMapping("/{id}/arbol")
    @Operation(summary = "Ver árbol de reclutados", description = "Obtiene el árbol de reclutados de un usuario.")
    public ResponseEntity<ApiResponse<ArbolUsuarioResponse>> obtenerArbol(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        // Admin puede ver cualquier árbol
        // Reclutador solo puede ver su propio árbol
        if (usuarioActual.getRol() != RolUsuario.ADMIN && !usuarioActual.getId().equals(id)) {
            throw new AccesoNoAutorizadoException("Solo puede ver su propio árbol de reclutados");
        }
        
        ArbolUsuarioResponse response = usuarioService.obtenerArbolReclutados(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/reclutados")
    @Operation(summary = "Ver reclutados directos", description = "Lista los reclutados directos de un usuario.")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> obtenerReclutadosDirectos(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        // Admin puede ver cualquiera, reclutador solo los suyos
        if (usuarioActual.getRol() != RolUsuario.ADMIN && !usuarioActual.getId().equals(id)) {
            throw new AccesoNoAutorizadoException("Solo puede ver sus propios reclutados");
        }
        
        List<UsuarioResponse> response = usuarioService.obtenerReclutadosDirectos(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Obtiene el perfil del usuario autenticado.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> miPerfil(
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        UsuarioResponse response = usuarioService.obtenerUsuario(usuarioActual.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/arbol")
    @Operation(summary = "Mi árbol", description = "Obtiene mi árbol de reclutados.")
    public ResponseEntity<ApiResponse<ArbolUsuarioResponse>> miArbol(
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        ArbolUsuarioResponse response = usuarioService.obtenerArbolReclutados(usuarioActual.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas del árbol", description = "Obtiene estadísticas generales del árbol de usuarios. Solo ADMIN.")
    public ResponseEntity<ApiResponse<EstadisticasArbolResponse>> obtenerEstadisticas(
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        if (usuarioActual.getRol() != RolUsuario.ADMIN) {
            throw new AccesoNoAutorizadoException("Solo el administrador puede ver las estadísticas");
        }
        
        EstadisticasArbolResponse response = usuarioService.obtenerEstadisticasArbol();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === Métoddo auxiliar ===
    
    /**
     * Verifica si un usuario está en la cadena de reclutados de otro (toda la cadena hacia abajo).
     */
    private boolean esReclutadoDe(Long usuarioId, Long posibleReclutadorId) {
        return usuarioService.verificarEsReclutadoDe(usuarioId, posibleReclutadorId);
    }
}
