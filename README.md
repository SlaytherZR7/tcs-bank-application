# Bank Application — Reto técnico de microservicios

Solución bancaria con dos microservicios Spring Boot que se comunican de forma asíncrona mediante RabbitMQ. Cada uno tiene su propia base PostgreSQL.

| Servicio          | Responsabilidad               | Puerto |
| ----------------- | ----------------------------- | ------ |
| `client-service`  | Persona y Cliente             | `8082` |
| `account-service` | Cuenta, Movimiento y Reportes | `8083` |

**Tecnologías:** Java 21, Spring Boot 4, PostgreSQL 16, RabbitMQ 4, Flyway, Docker Compose, JUnit 5, Mockito y Testcontainers.

## Levantar el proyecto

**Requisitos:** Docker con Docker Compose v2. No hace falta Java, porque las imágenes compilan los servicios.

```bash
# 1. Configurar variables de entorno (reemplaza las contraseñas replace-me-*)
cp .env.example .env

# 2. Construir y arrancar los servicios, las bases y RabbitMQ
docker compose up --build -d --wait

# 3. Verificar que estén sanos ("status":"UP")
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Flyway crea las tablas al arrancar, así que no hace falta ejecutar ningún script SQL.

### Probar con Postman

Importa [`postman_collection.json`](postman_collection.json) en Postman y ejecútala completa sobre un entorno limpio. También se puede ejecutar desde la terminal:

```bash
docker compose down -v && docker compose up --build -d --wait
npx newman run postman_collection.json
```

### Detener

```bash
docker compose down      # conserva los datos
docker compose down -v   # borra también las bases de datos
```

## Documentación

- [Arquitectura y decisiones de diseño](docs/architecture.md): consistencia eventual, concurrencia, historial de movimientos, rendimiento, escalabilidad y resiliencia.
- [API](docs/api.md): endpoints, códigos de error, contrato del reporte y ejemplos con curl.
- [Pruebas](docs/testing.md): pruebas unitarias, de integración y colección Postman.
- [Configuración y desarrollo](docs/development.md): variables de entorno, ejecución sin Docker, base de datos (`BaseDatos.sql`) y estructura del repositorio.
