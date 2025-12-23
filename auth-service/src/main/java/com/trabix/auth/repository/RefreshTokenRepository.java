package com.trabix.auth.repository;

import com.trabix.auth.entity.RefreshToken;
import com.trabix.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones de RefreshToken.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario = :usuario")
    void revocarTodosDelUsuario(Usuario usuario);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.fechaExpiracion < CURRENT_TIMESTAMP")
    void eliminarExpirados();
}
