package com.trabix.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de cuadres TRABIX.
 * Maneja: cálculo automático de cuadres, triggers por stock,
 * distribución según modelo (60/40 o 50/50 cascada),
 * generación de textos WhatsApp.
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.billing", "com.trabix.common"})
@EnableScheduling
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
