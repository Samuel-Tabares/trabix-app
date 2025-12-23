package com.trabix.auth.repository;

import com.trabix.auth.entity.Usuario;
import com.trabix.common.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones de Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCedula(String cedula);

    Optional<Usuario> findByCedulaAndEstado(String cedula, EstadoUsuario estado);

    boolean existsByCedula(String cedula);

    boolean existsByCorreo(String correo);
}
