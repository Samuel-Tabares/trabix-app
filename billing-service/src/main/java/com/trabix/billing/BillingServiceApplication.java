package com.trabix.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de cuadres TRABIX.
 * Puerto: 8084
 * 
 * Funcionalidades:
 * - Cálculo automático de cuadres
 * - Triggers por stock (20%)
 * - Distribución según modelo (60/40 o 50/50 cascada)
 * - Generación de textos WhatsApp
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.billing", "com.trabix.common"})
@EnableScheduling
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
