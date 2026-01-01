package com.trabix.auth.repository;

import com.trabix.auth.entity.Usuario;
import com.trabix.common.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Actualiza el contador de intentos fallidos.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = u.intentosFallidos + 1 WHERE u.cedula = :cedula")
    void incrementarIntentosFallidos(@Param("cedula") String cedula);

    /**
     * Resetea los intentos fallidos y registra login exitoso.
     */
    @Modifying
    @Query("""
        UPDATE Usuario u SET 
            u.intentosFallidos = 0, 
            u.bloqueadoHasta = NULL, 
            u.ultimoLogin = :fecha 
        WHERE u.cedula = :cedula
        """)
    void registrarLoginExitoso(@Param("cedula") String cedula, @Param("fecha") LocalDateTime fecha);

    /**
     * Bloquea temporalmente una cuenta.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.bloqueadoHasta = :hasta WHERE u.cedula = :cedula")
    void bloquearCuenta(@Param("cedula") String cedula, @Param("hasta") LocalDateTime hasta);

    /**
     * Actualiza la contraseña de un usuario.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.passwordHash = :passwordHash, u.updatedAt = :fecha WHERE u.id = :id")
    void actualizarPassword(@Param("id") Long id, @Param("passwordHash") String passwordHash, @Param("fecha") LocalDateTime fecha);
}
