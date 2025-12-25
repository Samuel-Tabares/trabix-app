package com.trabix.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway TRABIX.
 * Puerto: 8090
 * 
 * Punto de entrada único para todos los microservicios.
 * 
 * Rutas:
 * - /api/auth/**    → auth-service (8080)
 * - /api/users/**   → user-service (8081)
 * - /api/inventory/** → inventory-service (8082)
 * - /api/sales/**   → sales-service (8083)
 * - /api/billing/** → billing-service (8084)
 * - /api/finance/** → finance-service (8085)
 * - /api/equipment/** → equipment-service (8086)
 * - /api/documents/** → document-service (8087)
 * - /api/notifications/** → notification-service (8088)
 * - /api/backups/** → backup-service (8089)
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
