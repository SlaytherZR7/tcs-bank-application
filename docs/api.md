# API

Todas las rutas usan el prefijo `/api` y JSON con nombres en español. Las fechas de consulta usan el formato ISO `YYYY-MM-DD`.

| Recurso                                                                          | Métodos                                 | Servicio                 |
| -------------------------------------------------------------------------------- | --------------------------------------- | ------------------------ |
| `/api/clientes`, `/api/clientes/{id}`                                            | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `client-service` (8082)  |
| `/api/cuentas`, `/api/cuentas/{id}`, `/api/cuentas?clienteId={id}`               | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `account-service` (8083) |
| `/api/movimientos`, `/api/movimientos/{id}`, `/api/movimientos?numeroCuenta={n}` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `account-service` (8083) |
| `/api/reportes?clienteId={id}&fechaInicio={fecha}&fechaFin={fecha}`              | `GET`                                   | `account-service` (8083) |

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

| Código | Cuándo                                           |
| ------ | ------------------------------------------------ |
| `201`  | Recurso creado (incluye la cabecera `Location`)  |
| `200`  | Consulta o actualización correcta                |
| `204`  | Baja o anulación correcta                        |
| `400`  | Validación o JSON inválido                       |
| `404`  | Recurso inexistente                              |
| `409`  | Duplicado, saldo insuficiente o recurso inactivo |

## Reporte de estado de cuenta

Devuelve una fila por movimiento del rango, con el formato del enunciado:

```json
[
  {
    "Fecha": "10/2/2022",
    "Cliente": "Marianela Montalvo",
    "Numero Cuenta": "225487",
    "Tipo": "Corriente",
    "Saldo Inicial": 100.0,
    "Estado": true,
    "Movimiento": 600.0,
    "Saldo Disponible": 700.0
  }
]
```

- `fechaInicio` y `fechaFin` son obligatorias y ambas se incluyen en el rango.
- Las cuentas sin movimientos en el rango aparecen con `Movimiento` igual a `0`, `Fecha` igual a `fechaFin` y el saldo que tenían al cierre del período.

## Ejemplo de flujo con curl

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
