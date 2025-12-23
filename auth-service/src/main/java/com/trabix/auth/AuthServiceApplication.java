package com.trabix.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de autenticación y autorización TRABIX.
 * Maneja: login, JWT, roles, tokens de refresh.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.auth", "com.trabix.common"})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
