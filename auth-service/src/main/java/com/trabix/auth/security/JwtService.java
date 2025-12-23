package com.trabix.auth.security;

import com.trabix.auth.entity.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar y validar tokens JWT.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long jwtExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long jwtExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Genera un access token para el usuario.
     */
    public String generarAccessToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().name());
        claims.put("nivel", usuario.getNivel());
        claims.put("nombre", usuario.getNombre());
        
        return generarToken(claims, usuario.getCedula(), jwtExpiration);
    }

    /**
     * Genera un refresh token para el usuario.
     */
    public String generarRefreshToken(Usuario usuario) {
        return generarToken(new HashMap<>(), usuario.getCedula(), refreshExpiration);
    }

    /**
     * Genera un token con los claims especificados.
     */
    private String generarToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
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
     * Verifica si el token es válido para el usuario.
     */
    public boolean esTokenValido(String token, Usuario usuario) {
        try {
            final String cedula = extraerCedula(token);
            return cedula.equals(usuario.getCedula()) && !esTokenExpirado(token);
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

    /**
     * Obtiene el tiempo de expiración del access token.
     */
    public long getJwtExpiration() {
        return jwtExpiration;
    }

    /**
     * Obtiene el tiempo de expiración del refresh token.
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}
