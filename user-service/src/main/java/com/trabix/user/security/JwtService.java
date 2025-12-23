package com.trabix.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio para validar tokens JWT.
 * Solo valida, no genera (eso lo hace auth-service).
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extrae la cédula (subject) del token.
     */
    public String extraerCedula(String token) {
        return extraerClaims(token).getSubject();
    }

    /**
     * Extrae el rol del token.
     */
    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    /**
     * Extrae el nivel del token.
     */
    public String extraerNivel(String token) {
        return extraerClaims(token).get("nivel", String.class);
    }

    /**
     * Verifica si el token es válido (no expirado y firma correcta).
     */
    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return !esTokenExpirado(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el token ha expirado.
     */
    public boolean esTokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    public Date extraerExpiracion(String token) {
        return extraerClaims(token).getExpiration();
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
