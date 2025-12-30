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
 * - Triggers por stock (20% T2/T3) y por monto (T1)
 * - Distribución según modelo (60/40 o 50/50 cascada)
 * - Generación de textos WhatsApp
 * 
 * MODELO DE NEGOCIO:
 * - INVERSIÓN: Siempre 50/50 (Samuel y vendedor)
 * - GANANCIAS: 60/40 (N2) o 50/50 cascada (N3+)
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.billing", "com.trabix.common"})
@EnableScheduling
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
