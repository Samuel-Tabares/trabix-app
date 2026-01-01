package com.trabix.auth.repository;

import com.trabix.auth.entity.RefreshToken;
import com.trabix.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de RefreshToken.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Busca token válido (no revocado y no expirado).
     */
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.token = :token 
        AND rt.revocado = false 
        AND rt.fechaExpiracion > :ahora
        """)
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("ahora") LocalDateTime ahora);

    /**
     * Lista tokens activos de un usuario.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.usuario = :usuario 
        AND rt.revocado = false 
        AND rt.fechaExpiracion > :ahora
        """)
    List<RefreshToken> findActiveTokensByUsuario(@Param("usuario") Usuario usuario, @Param("ahora") LocalDateTime ahora);

    /**
     * Revoca todos los tokens de un usuario.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario = :usuario")
    void revocarTodosDelUsuario(@Param("usuario") Usuario usuario);

    /**
     * Revoca un token específico.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.token = :token")
    void revocarToken(@Param("token") String token);

    /**
     * Elimina tokens expirados (limpieza programada).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.fechaExpiracion < :fecha")
    int eliminarExpirados(@Param("fecha") LocalDateTime fecha);

    /**
     * Cuenta tokens activos de un usuario.
     */
    @Query("""
        SELECT COUNT(rt) FROM RefreshToken rt 
        WHERE rt.usuario.id = :usuarioId 
        AND rt.revocado = false 
        AND rt.fechaExpiracion > :ahora
        """)
    long contarTokensActivos(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
