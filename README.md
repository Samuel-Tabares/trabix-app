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

### 1. Clonar y configurar

```bash
# Copiar variables de entorno
cp .env.example .env

# Editar .env con tus valores
```

### 2. Levantar infraestructura

```bash
# Iniciar PostgreSQL y Redis
docker-compose up -d

# Verificar que estén corriendo
docker-compose ps
```

### 3. Compilar el proyecto

```bash
# Desde la raíz del proyecto
mvn clean install -DskipTests
```

### 4. Ejecutar auth-service

```bash
cd auth-service
mvn spring-boot:run
```

### 5. Probar

```bash
# Login (usuario admin)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cedula": "1092456501", "password": "Guta0214."}'
```

## 🔐 Usuario Admin Inicial

- **Cédula:** 1092456501
- **Contraseña:** Guta0214.
- **Rol:** ADMIN
- **Nivel:** N1

## 📚 Documentación API

Con el servicio corriendo, acceder a:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## 🐳 Docker (Desarrollo)

```bash
# Levantar todo (incluye Adminer para gestionar BD)
docker-compose --profile dev up -d

# Adminer: http://localhost:8081
# Sistema: PostgreSQL
# Servidor: postgres
# Usuario: trabix_admin
# Contraseña: trabix_secure_2024
# Base de datos: trabix_db
```

## 📋 Próximos Pasos

1. [x] Estructura base del proyecto
2. [x] Docker Compose (PostgreSQL + Redis)
3. [x] Módulo common
4. [x] Auth-service (JWT, login, roles)
5. [ ] User-service (CRUD, árbol cascada)
6. [ ] Inventory-service (stock, tandas, lotes)
7. [ ] Sales-service (ventas, aprobación)
8. [ ] Billing-service (cuadres automáticos)
9. [ ] Finance-service (costos, fondo)
10. [ ] Equipment-service (neveras, pijamas)
11. [ ] Document-service (cotizaciones, facturas)
12. [ ] Notification-service
13. [ ] Frontend React PWA

## 📄 Licencia

Proyecto privado - TRABIX Granizados © 2024
