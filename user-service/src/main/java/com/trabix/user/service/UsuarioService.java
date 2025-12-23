package com.trabix.user.service;

import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.enums.RolUsuario;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.user.dto.*;
import com.trabix.user.entity.Usuario;
import com.trabix.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios y árbol de cascada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CARACTERES_PASSWORD = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD = 8;

    /**
     * Crea un nuevo usuario/vendedor.
     */
    @Transactional
    public UsuarioCreadoResponse crearUsuario(CrearUsuarioRequest request) {
        // Validar que la cédula no exista
        if (usuarioRepository.existsByCedula(request.getCedula())) {
            throw new ValidacionNegocioException("Ya existe un usuario con la cédula: " + request.getCedula());
        }

        // Validar que el correo no exista
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ValidacionNegocioException("Ya existe un usuario con el correo: " + request.getCorreo());
        }

        // Determinar reclutador y nivel
        Usuario reclutador = null;
        String nivel;
        
        if (request.getReclutadorId() != null) {
            reclutador = usuarioRepository.findById(request.getReclutadorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Reclutador", request.getReclutadorId()));
            
            // Si el reclutador es VENDEDOR, promoverlo a RECLUTADOR
            if (reclutador.getRol() == RolUsuario.VENDEDOR) {
                reclutador.setRol(RolUsuario.RECLUTADOR);
                usuarioRepository.save(reclutador);
                log.info("Usuario {} promovido a RECLUTADOR", reclutador.getCedula());
            }
            
            // Calcular nivel: un nivel abajo del reclutador
            nivel = calcularNivelHijo(reclutador.getNivel());
        } else {
            // Sin reclutador = entra directo con admin = N2
            nivel = "N2";
        }

        // Generar o usar contraseña proporcionada
        String passwordPlano = request.getPassword();
        if (passwordPlano == null || passwordPlano.isBlank()) {
            passwordPlano = generarPassword();
        }

        // Crear usuario
        Usuario usuario = Usuario.builder()
                .cedula(request.getCedula())
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .passwordHash(passwordEncoder.encode(passwordPlano))
                .rol(RolUsuario.VENDEDOR)
                .nivel(nivel)
                .reclutador(reclutador)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado: {} - Nivel: {} - Reclutador: {}", 
                usuario.getCedula(), nivel, reclutador != null ? reclutador.getCedula() : "DIRECTO");

        // Construir respuesta
        UsuarioResponse usuarioResponse = mapToResponse(usuario);
        String mensajeWhatsApp = generarMensajeBienvenida(usuario, passwordPlano);

        return UsuarioCreadoResponse.builder()
                .usuario(usuarioResponse)
                .passwordGenerado(passwordPlano)
                .mensajeWhatsApp(mensajeWhatsApp)
                .build();
    }

    /**
     * Obtiene un usuario por ID.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        return mapToResponse(usuario);
    }

    /**
     * Obtiene un usuario por cédula.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorCedula(String cedula) {
        Usuario usuario = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", cedula));
        return mapToResponse(usuario);
    }

    /**
     * Lista usuarios con paginación.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable, boolean soloActivos) {
        Page<Usuario> usuarios;
        if (soloActivos) {
            usuarios = usuarioRepository.findByEstado(EstadoUsuario.ACTIVO, pageable);
        } else {
            usuarios = usuarioRepository.findAll(pageable);
        }
        return usuarios.map(this::mapToResponse);
    }

    /**
     * Lista vendedores (excluyendo admin).
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarVendedores(Pageable pageable) {
        return usuarioRepository.findVendedoresActivos(EstadoUsuario.ACTIVO, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Actualiza datos de un usuario.
     */
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        }
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            usuario.setTelefono(request.getTelefono());
        }
        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            // Validar que el nuevo correo no exista en otro usuario
            if (!usuario.getCorreo().equals(request.getCorreo()) 
                    && usuarioRepository.existsByCorreo(request.getCorreo())) {
                throw new ValidacionNegocioException("Ya existe un usuario con el correo: " + request.getCorreo());
            }
            usuario.setCorreo(request.getCorreo());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario actualizado: {}", usuario.getCedula());

        return mapToResponse(usuario);
    }

    /**
     * Desactiva un usuario (soft delete).
     */
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        if (usuario.getRol() == RolUsuario.ADMIN) {
            throw new ValidacionNegocioException("No se puede desactivar al administrador");
        }

        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: {}", usuario.getCedula());
    }

    /**
     * Reactiva un usuario.
     */
    @Transactional
    public UsuarioResponse reactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);
        log.info("Usuario reactivado: {}", usuario.getCedula());

        return mapToResponse(usuario);
    }

    /**
     * Obtiene el árbol de reclutados de un usuario.
     */
    @Transactional(readOnly = true)
    public ArbolUsuarioResponse obtenerArbolReclutados(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        return construirArbol(usuario);
    }

    /**
     * Obtiene los reclutados directos de un usuario.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerReclutadosDirectos(Long usuarioId) {
        // Verificar que el usuario existe
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("Usuario", usuarioId);
        }

        return usuarioRepository.findByReclutadorIdAndEstado(usuarioId, EstadoUsuario.ACTIVO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca usuarios por nombre.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarPorNombre(String nombre) {
        return usuarioRepository.buscarPorNombre(nombre, EstadoUsuario.ACTIVO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Resetea la contraseña de un usuario y genera una nueva.
     */
    @Transactional
    public String resetearPassword(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        String nuevoPassword = generarPassword();
        usuario.setPasswordHash(passwordEncoder.encode(nuevoPassword));
        usuarioRepository.save(usuario);

        log.info("Contraseña reseteada para usuario: {}", usuario.getCedula());
        return nuevoPassword;
    }

    // === Métodos privados ===

    private String calcularNivelHijo(String nivelPadre) {
        // Extraer número del nivel (N2 -> 2, N3 -> 3, etc.)
        int numeroNivel = Integer.parseInt(nivelPadre.substring(1));
        return "N" + (numeroNivel + 1);
    }

    private String generarPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(LONGITUD_PASSWORD);
        for (int i = 0; i < LONGITUD_PASSWORD; i++) {
            password.append(CARACTERES_PASSWORD.charAt(random.nextInt(CARACTERES_PASSWORD.length())));
        }
        return password.toString();
    }

    private String generarMensajeBienvenida(Usuario usuario, String password) {
        return String.format("""
            🍧 *¡Bienvenido a TRABIX Granizados!* 🍧
            
            Hola %s, ya eres parte del equipo.
            
            📱 *Tus credenciales de acceso:*
            • Cédula: %s
            • Contraseña: %s
            
            🔗 Ingresa a la app: [URL de la app]
            
            ⚠️ *Importante:* Cambia tu contraseña después del primer ingreso.
            
            ¡Éxitos en las ventas! 💪
            """,
            usuario.getNombre().split(" ")[0], // Solo primer nombre
            usuario.getCedula(),
            password
        );
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        UsuarioResponse.ReclutadorInfo reclutadorInfo = null;
        if (usuario.getReclutador() != null) {
            reclutadorInfo = UsuarioResponse.ReclutadorInfo.builder()
                    .id(usuario.getReclutador().getId())
                    .nombre(usuario.getReclutador().getNombre())
                    .nivel(usuario.getReclutador().getNivel())
                    .build();
        }

        long totalReclutados = usuarioRepository.countByReclutadorIdAndEstado(usuario.getId(), EstadoUsuario.ACTIVO);

        return UsuarioResponse.builder()
                .id(usuario.getId())
                .cedula(usuario.getCedula())
                .nombre(usuario.getNombre())
                .telefono(usuario.getTelefono())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .nivel(usuario.getNivel())
                .modeloNegocio(usuario.getModeloNegocio())
                .estado(usuario.getEstado())
                .fechaIngreso(usuario.getFechaIngreso())
                .reclutador(reclutadorInfo)
                .totalReclutados((int) totalReclutados)
                .build();
    }

    private ArbolUsuarioResponse construirArbol(Usuario usuario) {
        List<Usuario> reclutadosDirectos = usuarioRepository
                .findByReclutadorIdAndEstado(usuario.getId(), EstadoUsuario.ACTIVO);

        List<ArbolUsuarioResponse> hijosArbol = reclutadosDirectos.stream()
                .map(this::construirArbol) // Recursivo
                .collect(Collectors.toList());

        int totalDirectos = reclutadosDirectos.size();
        int totalIndirectos = hijosArbol.stream()
                .mapToInt(h -> h.getTotalDirectos() + h.getTotalIndirectos())
                .sum();

        return ArbolUsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .cedula(usuario.getCedula())
                .nivel(usuario.getNivel())
                .modeloNegocio(usuario.getModeloNegocio())
                .estado(usuario.getEstado().name())
                .reclutados(hijosArbol)
                .totalDirectos(totalDirectos)
                .totalIndirectos(totalIndirectos)
                .build();
    }
}
