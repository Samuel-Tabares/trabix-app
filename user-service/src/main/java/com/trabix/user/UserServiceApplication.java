package com.trabix.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de gestión de usuarios TRABIX.
 * Maneja: CRUD usuarios, árbol de cascada, niveles, roles.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.user", "com.trabix.common"})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
