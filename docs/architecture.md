# Decisiones de arquitectura

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

## Estructura y patrones

- **Microservicios con base de datos propia.** `client-service` agrupa Persona/Cliente y `account-service` agrupa Cuenta/Movimiento, como pide el reto. Ningún servicio lee la base del otro.
- **Arquitectura por capas** en cada servicio: `controller` → `service` → `repository`, con `model.entity`, `model.dto`, `mappers` y `exceptions`.
- **Patrones aplicados:**
  - Repository (Spring Data JPA);
  - DTO + Mapper (MapStruct), para no exponer entidades JPA;
  - manejo global de excepciones (`@RestControllerAdvice`) con una única estructura de error;
  - inyección por constructor;
  - herencia JPA `Persona` → `Cliente` (`@MappedSuperclass`).
- **Flyway** versiona el esquema y Hibernate solo lo valida (`ddl-auto: validate`).

## Comunicación asíncrona y consistencia eventual

- `client-service` publica `ClientCreated`, `ClientUpdated` y `ClientDeactivated` en el exchange `bank.client.events`. El evento sale **después del commit** (`@TransactionalEventListener(AFTER_COMMIT)`), así que una operación revertida nunca publica eventos.
- Los eventos no incluyen la contraseña. Llevan `eventId`, `schemaVersion` y `occurredAt` para versionar el contrato.
- `account-service` mantiene una **proyección local del cliente**. Es un **consumidor idempotente**: ignora un `eventId` ya procesado y descarta eventos más antiguos que el último aplicado.
- No hay llamadas síncronas entre servicios: si `client-service` cae, `account-service` sigue operando con su proyección. A cambio, la consistencia es eventual: una cuenta recién pedida para un cliente nuevo responde `404` hasta que llega su evento, y un cliente dado de baja responde `409`.

## Saldos y concurrencia

- Los importes usan `BigDecimal`, con `NUMERIC(19,2)` en PostgreSQL.
- Cada movimiento lee la cuenta con **bloqueo pesimista** (`SELECT … FOR UPDATE`), valida el saldo, registra el movimiento y actualiza el saldo **en una sola transacción**.
- Dos retiros simultáneos se ejecutan uno detrás de otro, nunca a la vez, así que no pueden gastar el mismo saldo. Una prueba de integración lanza 10 retiros concurrentes contra PostgreSQL real: se aceptan exactamente los que caben.
- El bloqueo es por fila de cuenta, así que solo compiten las operaciones sobre la misma cuenta.
- Un retiro sin fondos responde `409` con el mensaje `Saldo no disponible` y no deja cambios parciales.

## Historial de movimientos de solo inserción

Un movimiento registrado nunca se edita ni se borra físicamente, porque el historial es un registro contable auditable:

- `PUT`/`PATCH` registran una **reversión** del original y un **reemplazo** con el nuevo valor. Un movimiento ya corregido no admite otra corrección (`409`).
- `DELETE` registra un **movimiento compensatorio** y es idempotente.
- Cada movimiento guarda el saldo resultante, así que el saldo de cualquier fecha se obtiene del último movimiento hasta ese momento.

Las fechas de los movimientos se generan con un `Clock` en la zona del banco (`BANK_TIME_ZONE`), no en la de la JVM, que dentro de los contenedores es UTC.

## Contrato del reporte

- El enunciado sugiere `/reportes?fecha=rango fechas` y deja a criterio la forma de pedirlo. Se usa `clienteId`, `fechaInicio` y `fechaFin` con fechas ISO: es explícito, se valida con Bean Validation y no obliga a interpretar un formato de rango propio.
- La respuesta mantiene el formato plano del ejemplo del enunciado: una fila por movimiento, con las claves `Fecha`, `Cliente`, `Numero Cuenta`, etc.
- Las cuentas sin movimientos en el período también aparecen, con su saldo al cierre. Así el reporte cubre "cuentas asociadas con sus respectivos saldos".
- Usa un número fijo de consultas (3 o 4) sin importar cuántas cuentas tenga el cliente, para evitar el problema N+1.

## Rendimiento

- Hay índices en `accounts.client_id`, `movements.account_id`, `movements.movement_date` y las claves únicas.
- El reporte se resuelve con consultas por lote, sin N+1. `open-in-view` está desactivado, así que no hay consultas perezosas fuera del servicio.
- Las imágenes Docker se construyen por capas (las dependencias se cachean aparte) y la JVM respeta el límite de memoria del contenedor (`MaxRAMPercentage`).
- **Evolución:** paginar los listados y el reporte, poner un rango máximo de fechas y servir los reportes desde una réplica de lectura.

## Escalabilidad

- Los servicios no guardan estado entre peticiones, así que se pueden replicar horizontalmente detrás de un balanceador.
- Varias réplicas de `account-service` consumen la misma cola como consumidores competidores. La idempotencia por `eventId` evita efectos duplicados si un evento se entrega más de una vez.
- Cada base escala según la carga de su servicio.
- El bloqueo pesimista es por cuenta, así que no serializa operaciones de cuentas distintas.

## Resiliencia

- **Contenedores:** cada servicio tiene un healthcheck (`/actuator/health`, que incluye la base y RabbitMQ), `restart: unless-stopped` y un orden de arranque basado en `depends_on: service_healthy`.
- **Mensajería:**
  - exchange, colas y mensajes durables;
  - el consumidor **reintenta** 3 veces con espera exponencial (1 s, 2 s);
  - si sigue fallando, el mensaje va a la **DLQ** (`account.client-lifecycle.dlq`) en lugar de perderse o reintentarse para siempre;
  - los mensajes ilegibles se rechazan y van directamente a la DLQ.
- **Operación de la DLQ:** la consola de RabbitMQ está en `http://localhost:15672`, con las credenciales `RABBITMQ_DEFAULT_*`. En **Queues → `account.client-lifecycle.dlq` → Get messages** se inspecciona el mensaje. Una vez corregida la causa, se reprocesa publicándolo de nuevo en el exchange `bank.client.events` con la routing key `client.lifecycle.v1`. Como el consumidor es idempotente, reprocesar no duplica efectos.
- **Riesgo conocido: patrón Outbox.** Hoy el evento se publica después del commit. Si RabbitMQ no está disponible en ese momento, el cambio queda guardado en la base pero el evento se pierde; el fallo solo queda registrado en el log. La solución es el **patrón Transactional Outbox**:
  1. guardar el evento en una tabla `outbox` dentro de la misma transacción que el cambio del cliente;
  2. un proceso aparte, programado o con CDC tipo Debezium, lo publica y lo marca como enviado.

  Esto garantiza entrega *at-least-once*, que ya es segura gracias al consumidor idempotente.

## Seguridad

- Las contraseñas se guardan con **BCrypt** y nunca aparecen en respuestas, eventos ni logs. Los logs solo registran identificadores técnicos.
- Las credenciales se leen de variables de entorno (`.env`, fuera del repositorio).
- La autenticación y la autorización quedan fuera del alcance del reto. La evolución natural sería un API Gateway con OAuth2/JWT.
