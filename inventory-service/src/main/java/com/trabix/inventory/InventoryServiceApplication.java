package com.trabix.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de inventario TRABIX.
 * Maneja: lotes, tandas, stock, liberación de tandas.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.inventory", "com.trabix.common"})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
