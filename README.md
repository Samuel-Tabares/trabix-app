# TRABIX GRANIZADOS - Sistema de Gestión

Sistema de gestión integral para la operación de venta de granizados con modelo multinivel en cascada.

## 🚀 Tecnologías

- **Backend:** Java 17 + Spring Boot 3.2
- **Base de datos:** PostgreSQL 15
- **Caché:** Redis 7
- **Autenticación:** JWT
- **Contenedores:** Docker + Docker Compose

## 📁 Estructura del Proyecto

```
trabix-app/
├── common/                 # Módulo compartido (DTOs, excepciones, enums)
├── auth-service/           # Autenticación y autorización
├── user-service/           # Gestión de usuarios y árbol cascada
├── inventory-service/      # Stock, tandas, lotes
├── sales-service/          # Registro y aprobación de ventas
├── billing-service/        # Cuadres automáticos
├── finance-service/        # Costos y fondo de recompensas
├── equipment-service/      # Neveras y pijamas
├── document-service/       # Cotizaciones y facturas
├── notification-service/   # Push, email, textos WhatsApp
├── backup-service/         # Backup automático
├── gateway-service/        # API Gateway
├── scripts/                # Scripts SQL y utilidades
├── docker-compose.yml      # Servicios de infraestructura
└── pom.xml                 # POM padre Maven
```

## 🛠️ Requisitos

- Java 17+
- Maven 3.8+
- Docker y Docker Compose
- PostgreSQL 15 (o usar Docker)

## ⚡ Inicio Rápido

## 🔐 Usuario Admin Inicial

- **Cédula:** 1092456501
- **Contraseña:** Guta0214.
- **Rol:** ADMIN
- **Nivel:** N1

Con el servicio corriendo, acceder a:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
