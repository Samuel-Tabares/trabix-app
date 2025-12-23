package com.trabix.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de ventas TRABIX.
 * Maneja: registro de ventas, tipos de venta, aprobación, estadísticas.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.sales", "com.trabix.common"})
public class SalesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesServiceApplication.class, args);
    }
}
