# Configuración y desarrollo

## Variables de entorno

Edita `.env` y reemplaza las contraseñas de ejemplo (`replace-me-*`). El archivo `.env` está en `.gitignore` y nunca debe subirse al repositorio.

| Variable                                          | Uso                                               | Valor por defecto   |
| ------------------------------------------------- | ------------------------------------------------- | ------------------- |
| `CLIENT_SERVER_PORT` / `ACCOUNT_SERVER_PORT`      | Puertos publicados de los servicios               | `8082` / `8083`     |
| `CLIENT_DB_*` / `ACCOUNT_DB_*`                    | Nombre, usuario, contraseña y puerto de cada base | ver `.env.example`  |
| `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` | Credenciales del broker                           | `bank_app` / —      |
| `RABBITMQ_MANAGEMENT_PORT` | Puerto de la consola de RabbitMQ | `15672` |
| `BANK_TIME_ZONE`                                  | Zona horaria con la que se fechan los movimientos | `America/Guayaquil` |

## Arranque con Docker Compose

`--wait` deja la terminal bloqueada hasta que los cinco contenedores estén sanos (`healthy`). La primera vez tarda unos minutos porque descarga las imágenes y las dependencias de Gradle.

Orden de arranque:

1. `client-db`, `account-db` y `rabbitmq`.
2. `account-service`, que declara la cola de eventos de cliente.
3. `client-service`, que espera a `account-service` para no publicar eventos antes de que exista la cola.

Flyway crea las tablas automáticamente al arrancar cada servicio; no hace falta ejecutar ningún script SQL.

Para ver los logs de un servicio:

```bash
docker compose logs -f account-service
```

## Ejecutar los servicios fuera de Docker

Requiere **JDK 21**. Levanta solo la infraestructura con Compose y ejecuta los servicios con el perfil `local`:

```bash
docker compose up -d --wait client-db account-db rabbitmq

# En una terminal, desde la raíz del repositorio
set -a; . ./.env; set +a
cd account-service && ./gradlew bootRun --args='--spring.profiles.active=local'

# En otra terminal, desde la raíz del repositorio
set -a; . ./.env; set +a
cd client-service && ./gradlew bootRun --args='--spring.profiles.active=local'
```

El perfil `local` se conecta a `localhost` usando los puertos y credenciales de `.env`.

## Base de datos

- Las migraciones de Flyway están en `*/src/main/resources/db/migration` y se aplican al arrancar cada servicio.
- [`BaseDatos.sql`](../BaseDatos.sql) reúne esas migraciones en un solo script para crear ambas bases manualmente:

```bash
psql -U postgres -f BaseDatos.sql
```

## Estructura del repositorio

```
.
├── client-service/      # Microservicio de clientes (Dockerfile, Gradle, código y pruebas)
├── account-service/     # Microservicio de cuentas, movimientos y reportes
├── compose.yml          # Bases de datos, RabbitMQ y ambos servicios
├── .env.example         # Plantilla de variables de entorno (sin secretos reales)
├── BaseDatos.sql        # Esquema completo de ambas bases
└── postman_collection.json  # Colección Postman con los casos de uso del enunciado
```
