package com.trabix.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de finanzas TRABIX.
 * Maneja: configuración de costos variables, fondo de recompensas,
 * movimientos del fondo, registro de costos de producción.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.finance", "com.trabix.common"})
@EnableScheduling
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
