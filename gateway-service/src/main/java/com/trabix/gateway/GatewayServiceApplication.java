package com.trabix.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway TRABIX.
 * Puerto: 8090
 * 
 * Punto de entrada único para todos los microservicios.
 * 
 * ╔═══════════════════════════════════════════════════════════════╗
 * ║                    MAPA DE RUTAS                              ║
 * ╠═══════════════════════════════════════════════════════════════╣
 * ║ /api/auth/**          → auth-service        (8080)            ║
 * ║ /api/usuarios/**      → user-service        (8081)            ║
 * ║ /api/lotes/**         → inventory-service   (8082)            ║
 * ║ /api/tandas/**        → inventory-service   (8082)            ║
 * ║ /api/stock-produccion/** → inventory-service (8082)           ║
 * ║ /api/ventas/**        → sales-service       (8083)            ║
 * ║ /api/cuadres/**       → billing-service     (8084)            ║
 * ║ /api/costos/**        → finance-service     (8085)            ║
 * ║ /api/fondo/**         → finance-service     (8085)            ║
 * ║ /api/asignaciones/**  → equipment-service   (8086)            ║
 * ║ /api/mensualidades/** → equipment-service   (8086)            ║
 * ║ /api/stock/**         → equipment-service   (8086)            ║
 * ║ /api/documentos/**    → document-service    (8087)            ║
 * ║ /api/notificaciones/** → notification-service (8088)          ║
 * ║ /api/backups/**       → backup-service      (8089)            ║
 * ╚═══════════════════════════════════════════════════════════════╝
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
