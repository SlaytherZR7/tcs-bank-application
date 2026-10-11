# Bank Application — Reto técnico de microservicios

Solución bancaria con dos microservicios Spring Boot que se comunican de forma asíncrona mediante RabbitMQ:

| Servicio | Responsabilidad | Puerto | Base de datos |
|---|---|---|---|
| `client-service` | Persona y Cliente | `8082` | PostgreSQL `clientdb` |
| `account-service` | Cuenta, Movimiento y Reportes | `8083` | PostgreSQL `accountdb` |

```
                 ┌──────────────────┐   ClientCreated / ClientUpdated /   ┌───────────────────┐
  /api/clientes  │  client-service  │   ClientDeactivated (RabbitMQ)      │  account-service  │  /api/cuentas
 ───────────────►│                  ├────────────────────────────────────►│                   │◄─────────────── /api/movimientos
                 └────────┬─────────┘     exchange bank.client.events     └─────────┬─────────┘  /api/reportes
                          │                                                         │
                     ┌────▼─────┐                                              ┌────▼──────┐
                     │ clientdb │                                              │ accountdb │
                     └──────────┘                                              └───────────┘
```

- Cada servicio tiene su propia base de datos; no hay claves foráneas ni consultas entre bases.
- `account-service` mantiene una **proyección local del cliente** que actualiza con los eventos de `client-service`. La consistencia es eventual: una cuenta solo se puede crear cuando el evento del cliente ya llegó.
- Los movimientos se registran en un historial de solo inserción: `PUT`/`PATCH` registran una reversión y un reemplazo, y `DELETE` registra un movimiento compensatorio.

**Tecnologías:** Java 21, Spring Boot 4.0.8, Gradle 9.7.1, Spring Data JPA, Flyway, PostgreSQL 16, RabbitMQ 4, MapStruct, Lombok, JUnit 5 y Mockito.

---

## Levantar el proyecto con Docker (recomendado)

### Requisitos

- Docker con Docker Compose v2 (`docker compose version`).
- Puertos libres en el host: `8082`, `8083`, `5432`, `5433` y `5672`. Se pueden cambiar en `.env`.

No hace falta tener Java instalado: las imágenes compilan los servicios dentro de Docker.

### 1. Configurar las variables de entorno

```bash
cp .env.example .env
```

Edita `.env` y reemplaza las contraseñas de ejemplo (`replace-me-*`). El archivo `.env` está en `.gitignore` y nunca debe subirse al repositorio.

| Variable | Uso | Valor por defecto |
|---|---|---|
| `CLIENT_SERVER_PORT` / `ACCOUNT_SERVER_PORT` | Puertos publicados de los servicios | `8082` / `8083` |
| `CLIENT_DB_*` / `ACCOUNT_DB_*` | Nombre, usuario, contraseña y puerto de cada base | ver `.env.example` |
| `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` | Credenciales del broker | `bank_app` / — |
| `BANK_TIME_ZONE` | Zona horaria con la que se fechan los movimientos | `America/Guayaquil` |

### 2. Construir y arrancar

```bash
docker compose up --build -d --wait
```

`--wait` deja la terminal bloqueada hasta que los cinco contenedores estén sanos (`healthy`). La primera vez tarda unos minutos porque descarga las imágenes y las dependencias de Gradle.

Orden de arranque:

1. `client-db`, `account-db` y `rabbitmq`.
2. `account-service`, que declara la cola de eventos de cliente.
3. `client-service`, que espera a `account-service` para no publicar eventos antes de que exista la cola.

Flyway crea las tablas automáticamente al arrancar cada servicio; no hace falta ejecutar ningún script SQL.

### 3. Verificar

```bash
docker compose ps
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Ambos endpoints deben responder con `"status":"UP"`.

### 4. Probar el flujo completo

```bash
# 1. Crear un cliente
curl -X POST http://localhost:8082/api/clientes \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Marianela Montalvo","genero":"Femenino","edad":30,"identificacion":"1798765432",
       "direccion":"Amazonas y NNUU","telefono":"097548965","contrasena":"5678","estado":true}'

# 2. Crear una cuenta (usa el clienteId devuelto en el paso anterior)
curl -X POST http://localhost:8083/api/cuentas \
  -H 'Content-Type: application/json' \
  -d '{"numeroCuenta":"225487","tipoCuenta":"Corriente","saldoInicial":100,"estado":true,"clienteId":1}'

# 3. Registrar movimientos (valor positivo = depósito, negativo = retiro)
curl -X POST http://localhost:8083/api/movimientos \
  -H 'Content-Type: application/json' \
  -d '{"numeroCuenta":"225487","valor":600}'

# 4. Retiro sin fondos → 409 "Saldo no disponible"
curl -X POST http://localhost:8083/api/movimientos \
  -H 'Content-Type: application/json' \
  -d '{"numeroCuenta":"225487","valor":-1000}'

# 5. Reporte de estado de cuenta (el rango debe incluir la fecha de los movimientos)
curl "http://localhost:8083/api/reportes?clienteId=1&fechaInicio=2026-10-01&fechaFin=2026-10-31"
```

Si la creación de la cuenta responde `404` justo después de crear el cliente, el evento todavía no ha llegado a `account-service`: repite la petición un instante después.

### 5. Detener y limpiar

```bash
docker compose down        # detiene y elimina los contenedores; conserva los datos
docker compose down -v     # además elimina los volúmenes: borra todas las bases y la cola
```

Para ver los logs de un servicio:

```bash
docker compose logs -f account-service
```

---

## Ejecutar los servicios fuera de Docker (desarrollo)

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

---

## Pruebas

```bash
cd client-service && ./gradlew test
cd account-service && ./gradlew test
```

Las pruebas usan H2 en memoria y no necesitan Docker, PostgreSQL ni RabbitMQ. Incluyen pruebas unitarias de dominio y de servicios (JUnit 5 + Mockito), pruebas de endpoints (`@WebMvcTest` + MockMvc) y pruebas de repositorios (`@DataJpaTest`).

---

## API

Todas las rutas usan el prefijo `/api` y JSON con nombres en español. Las fechas de consulta usan el formato ISO `YYYY-MM-DD`.

| Recurso | Métodos | Servicio |
|---|---|---|
| `/api/clientes`, `/api/clientes/{id}` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `client-service` (8082) |
| `/api/cuentas`, `/api/cuentas/{id}`, `/api/cuentas?clienteId={id}` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `account-service` (8083) |
| `/api/movimientos`, `/api/movimientos/{id}`, `/api/movimientos?numeroCuenta={n}` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `account-service` (8083) |
| `/api/reportes?clienteId={id}&fechaInicio={fecha}&fechaFin={fecha}` | `GET` | `account-service` (8083) |

- `DELETE` de clientes y cuentas es una baja lógica: el registro y su historial se conservan.
- Los errores devuelven siempre la misma estructura:

```json
{
  "timestamp": "2026-10-11T19:23:07.781Z",
  "status": 409,
  "error": "Conflict",
  "message": "Saldo no disponible",
  "path": "/api/movimientos"
}
```

| Código | Cuándo |
|---|---|
| `201` | Recurso creado (incluye la cabecera `Location`) |
| `200` | Consulta o actualización correcta |
| `204` | Baja o anulación correcta |
| `400` | Validación o JSON inválido |
| `404` | Recurso inexistente |
| `409` | Duplicado, saldo insuficiente o recurso inactivo |

### Reporte de estado de cuenta

Devuelve una fila por movimiento del rango, con el formato del enunciado:

```json
[
  {
    "Fecha": "10/2/2022",
    "Cliente": "Marianela Montalvo",
    "Numero Cuenta": "225487",
    "Tipo": "Corriente",
    "Saldo Inicial": 100.00,
    "Estado": true,
    "Movimiento": 600.00,
    "Saldo Disponible": 700.00
  }
]
```

- `fechaInicio` y `fechaFin` son obligatorias y ambas se incluyen en el rango.
- Las cuentas sin movimientos en el rango aparecen con `Movimiento` igual a `0`, `Fecha` igual a `fechaFin` y el saldo que tenían al cierre del período.

---

## Base de datos

- Las migraciones de Flyway están en `*/src/main/resources/db/migration` y se aplican al arrancar cada servicio.
- [`BaseDatos.sql`](BaseDatos.sql) reúne esas migraciones en un solo script para crear ambas bases manualmente:

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
└── BaseDatos.sql        # Esquema completo de ambas bases
```
