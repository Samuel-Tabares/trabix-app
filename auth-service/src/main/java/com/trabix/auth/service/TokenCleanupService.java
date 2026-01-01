package com.trabix.auth.service;

import com.trabix.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio programado para limpieza de tokens expirados.
 * Se ejecuta diariamente a las 3:00 AM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Elimina tokens expirados de la base de datos.
     * Se ejecuta diariamente a las 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void limpiarTokensExpirados() {
        int eliminados = refreshTokenRepository.eliminarExpirados(LocalDateTime.now());
        
        if (eliminados > 0) {
            log.info("🧹 Limpieza de tokens: {} tokens expirados eliminados", eliminados);
        }
    }

    /**
     * Ejecuta limpieza manual (para uso administrativo).
     */
    @Transactional
    public int limpiarManualmente() {
        int eliminados = refreshTokenRepository.eliminarExpirados(LocalDateTime.now());
        log.info("🧹 Limpieza manual: {} tokens eliminados", eliminados);
        return eliminados;
    }
}
