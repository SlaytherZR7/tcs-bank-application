# Pruebas

## Pruebas automatizadas

| Comando (en cada servicio)  | Qué ejecuta                                                                                                                                      | Requiere Docker |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | --------------- |
| `./gradlew test`            | Pruebas unitarias de dominio y servicios (JUnit 5 + Mockito), de endpoints (`@WebMvcTest` + MockMvc) y de repositorios (`@DataJpaTest` sobre H2) | No              |
| `./gradlew integrationTest` | Pruebas de integración (`@Tag("integration")`) con PostgreSQL y RabbitMQ reales levantados por Testcontainers                                    | Sí              |
| `./gradlew build`           | Ambas                                                                                                                                            | Sí              |

```bash
cd client-service && ./gradlew test integrationTest
cd account-service && ./gradlew test integrationTest
```

Las pruebas de integración arrancan cada servicio con su configuración real (Flyway, listener de RabbitMQ) usando las mismas imágenes que `compose.yml`:

- **`client-service`**: flujo HTTP → servicio → repositorio → PostgreSQL. Comprueba la contraseña guardada con BCrypt, la publicación de `ClientCreated`/`ClientDeactivated` sin contraseña, y que un duplicado no publica eventos.
- **`account-service`**: el evento del cliente llega por RabbitMQ y habilita cuentas, movimientos y el reporte. Comprueba `Saldo no disponible` sin cambios parciales, retiros concurrentes sin sobregiro (bloqueo pesimista), la baja del cliente y el envío de eventos inválidos a la DLQ.

Cada lado de la mensajería se prueba contra un broker real con el contrato JSON compartido. El recorrido completo entre los dos servicios se verifica con el despliegue de Docker Compose.

## Validación con Postman

[`postman_collection.json`](../postman_collection.json) (formato v2.1, compatible con Postman v9.13.2) recorre los casos de uso del enunciado con aserciones automáticas:

| Carpeta | Contenido |
|---|---|
| `00 - Salud` | Healthchecks de ambos servicios |
| `01 - Clientes` | Jose Lema, Marianela Montalvo y Juan Osorio |
| `02 - Cuentas` | 478758, 225487, 495878, 496825 y la nueva cuenta corriente 585545 de Jose Lema |
| `03 - Movimientos` | Retiro de 575, depósitos de 600 y 150, retiro de 540 y retiro sin fondos (`Saldo no disponible`) |
| `04 - Reportes` | Estado de cuenta de Marianela Montalvo y de Jose Lema |
| `05 - CRUD complementario` | `PUT`, `PATCH` y `DELETE` de clientes, cuentas y movimientos sobre datos temporales |
| `06 - Manejo de errores` | Duplicados, validaciones, recursos inexistentes y errores del reporte |

Los casos de uso crean datos con identificaciones y números de cuenta fijos, así que la colección debe ejecutarse **completa, en orden y sobre un entorno limpio**:

```bash
docker compose down -v && docker compose up --build -d --wait
```

Después, en Postman: **Import** → `postman_collection.json` → **Run collection**. También se puede ejecutar desde la terminal con Newman:

```bash
npx newman run postman_collection.json
```

- Las variables `servidor`, `puertoClientes` y `puertoCuentas` definen las URLs (`http://{servidor}:{puerto}/api/...`). Los IDs que se crean se guardan automáticamente en variables de la colección.
- Si una cuenta se crea justo después de su cliente y responde `404`, el evento aún no llegó a `account-service`; la petición se reintenta sola.
