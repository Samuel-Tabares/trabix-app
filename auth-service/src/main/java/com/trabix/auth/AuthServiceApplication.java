package com.trabix.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de autenticación y autorización TRABIX.
 * Puerto: 8080
 * 
 * Funcionalidades:
 * - Login con cédula/password
 * - Generación de tokens JWT (access + refresh)
 * - Renovación de tokens
 * - Logout con revocación
 * - Cambio de contraseña
 * - Protección contra fuerza bruta
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.auth", "com.trabix.common"})
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
