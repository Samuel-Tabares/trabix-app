package com.trabix.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de finanzas TRABIX.
 * Puerto: 8085
 * 
 * Funcionalidades:
 * - Configuración de costos (real vs percibido)
 * - Fondo de recompensas ($200 por TRABIX)
 * - Registro de costos de producción
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.finance", "com.trabix.common"})
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
