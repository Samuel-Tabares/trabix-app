package com.trabix.finance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "TRABIX Finance Service API",
        version = "1.0.0",
        description = """
            Servicio de finanzas para el sistema TRABIX.
            
            ## Funcionalidades
            
            ### Configuración de Costos (Solo Admin)
            - Gestión del costo real vs percibido por TRABIX
            - Configuración de aportes al fondo y gestión
            
            ### Fondo de Recompensas
            - Saldo alimentado por $200 por cada TRABIX vendido
            - Ingreso/Retiro de dinero (Solo Admin)
            - Entrega de premios a usuarios (Solo Admin)
            - Consulta de movimientos (Todos)
            
            ### Costos de Producción (Solo Admin)
            - Registro de gastos: PRODUCCION, INSUMO, MARKETING, OTRO
            - Reportes y estadísticas por tipo y período
            """,
        contact = @Contact(
            name = "TRABIX Support",
            email = "trabixgranizados@gmail.com"
        )
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenido de auth-service"
)
public class OpenApiConfig {
}
