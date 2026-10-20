# Restaurante – Pedidos (REST + consola con ORM Hibernate/JPA)

Mismo estilo que `restaurante-rest`: acceso a datos con **Hibernate/JPA** (`GenericDAO`, `JPAUtil`, `persistence.xml`),
un **controlador REST genérico** (`CrudController`) y un **menú de consola con teclado**. Se agregó la lógica de las
5 funcionalidades y el control de acceso por rol.

## Las 5 funcionalidades

| Funcionalidad | Entidades con CRUD | Dónde está la lógica |
|---|---|---|
| **RF-01** Autenticación de usuarios (cocina, administración, mesero) | `rol`, `usuario` | `AuthService`, `Permisos`, `AuthInterceptor` |
| **RF-02** Gestión de pedidos (el mesero crea pedidos) | `pedido`, `historial_estado` | `PedidoService` |
| **RF-03** Asignación de mesa | `mesa` (y `pedido`) | `PedidoService.asignarMesa` |
| **RF-04** Productos del pedido | `detalle_pedido`, `producto`, `categoria` | `DetallePedidoService` |
| **RF-05** Observaciones del producto | columna `detalle_pedido.notas` | `DetallePedidoService.registrarObservacion` |

`factura` y `menu` no están porque ninguna de las 5 funcionalidades las usa.

## Estructura
```
src/main/java
  model/            entidades JPA (8 tablas)
  dao/              GenericDAO<T> (CRUD) y UsuarioDAO (cifra la contraseña con BCrypt)
  util/             JPAUtil (EntityManagerFactory + enLectura/enTransaccion) y Semilla (datos iniciales)
  security/         TipoRol, Sesion, Permisos (qué rol puede usar qué ruta)
  service/          AuthService, PedidoService, DetallePedidoService, Estados, ServicioException  <- reglas de negocio
  api/              RestauranteApplication y ManejadorErrores
  api/security/     AuthInterceptor (exige token y rol) y WebConfig
  api/controller/   CrudController genérico + un controlador por tabla + AuthController
  main/             Main (login), MenuAdmin, MenuMesero, MenuCocina, ConsolaCrud, Entrada, Vista
src/main/resources/META-INF/persistence.xml
sql/restaurante.sql                     DDL de referencia (Hibernate crea las tablas solo)
postman/Restaurante.postman_collection.json
```

## Cómo ejecutar
1. JDK 17+, Maven y MySQL en marcha.
2. Pon tu usuario y clave en `persistence.xml` (`jdbc.user` / `jdbc.password`) **o** define `DB_URL`, `DB_USER`, `DB_PASSWORD`.
   La base `restaurante` se crea sola.
3. **API REST:** `mvn spring-boot:run` (o ejecuta `api.RestauranteApplication`) → `http://localhost:8080`
4. **Consola con teclado:** `mvn compile exec:java` (o ejecuta `main.Main`)

La primera vez se cargan solos (`Semilla`) los roles, estos usuarios, 5 mesas, 2 categorías y 4 productos:

| Usuario | Contraseña | Rol |
|---|---|---|
| admin | admin123 | Administrador |
| mesero | mesero123 | Mesero |
| cocina | cocina123 | Cocina |

## RF-01: login y permisos por rol

Consola: al abrir `Main` pide usuario y contraseña y muestra el menú de su rol.

API: `POST /api/auth/login` con `{"usuarioLogin": "mesero", "contrasena": "mesero123"}` devuelve un `token`.
Todas las demás peticiones llevan el header `Authorization: Bearer <token>`.
Sin token → **401**. Con un rol sin permiso → **403**. Las contraseñas se guardan con BCrypt y las sesiones duran 8 horas
(viven en memoria: al reiniciar el servidor hay que volver a iniciar sesión).

| Recurso | Administración | Mesero | Cocina |
|---|---|---|---|
| roles, usuarios | todo | – | – |
| mesas, categorías, productos, historial | todo | consultar | consultar |
| pedidos | todo | consultar y gestionar los suyos | consultar y pasar a EN_PREPARACION / LISTO |
| detalles-pedido | todo | consultar y gestionar los de sus pedidos | consultar |

## Endpoints

Cada tabla tiene los 6 verbos (`/api/roles`, `/usuarios`, `/mesas`, `/categorias`, `/productos`, `/pedidos`,
`/detalles-pedido`, `/historial-estados`):

| Verbo | Ruta | Acción | Respuesta |
|---|---|---|---|
| GET | `/api/{recurso}` | listar todos | 200 |
| GET | `/api/{recurso}/{id}` | buscar uno | 200 / 404 |
| POST | `/api/{recurso}` | crear | 201 / 400 |
| PUT | `/api/{recurso}/{id}` | reemplazar | 200 / 404 / 400 |
| PATCH | `/api/{recurso}/{id}` | cambiar solo los campos enviados | 200 / 404 / 400 |
| DELETE | `/api/{recurso}/{id}` | eliminar | 204 / 404 / 409 |

Las llaves foráneas se envían como objeto con su id: `"mesa": {"idMesa": 1}`. Las fechas van como `2026-10-12T19:00:00`.
Al crear un usuario, la contraseña va en `contrasenaHash` (el sistema la cifra; nunca se devuelve).

Endpoints propios de las funcionalidades:

| Verbo | Ruta | Funcionalidad |
|---|---|---|
| POST | `/api/auth/login` · GET `/api/auth/me` · POST `/api/auth/logout` | RF-01 |
| POST | `/api/pedidos` `{"mesa":{"idMesa":1},"numPersonas":2}` | RF-02 crear pedido |
| PATCH | `/api/pedidos/{id}/estado` `{"estado":"LISTO"}` | RF-02 avanzar estado (queda en el historial) |
| GET | `/api/pedidos?estado=PENDIENTE` · `/api/pedidos/{id}/detalles` | RF-02 consultar |
| PUT | `/api/pedidos/{id}/mesa` `{"idMesa":3}` | RF-03 asignar el pedido a otra mesa |
| POST | `/api/detalles-pedido` `{"pedido":{"idPedido":1},"producto":{"idProducto":2},"cantidad":2,"notas":"Sin cebolla"}` | RF-04 agregar producto |
| GET | `/api/detalles-pedido?idPedido=1` | RF-04 productos de un pedido |
| PATCH | `/api/detalles-pedido/{id}/notas` `{"notas":"Sin picante"}` | RF-05 observación |

## Reglas de negocio
- Un pedido nace `PENDIENTE`, a nombre del mesero con sesión iniciada, y siempre tiene una mesa.
- La mesa debe estar `LIBRE` y tener capacidad para las personas; al asignarla pasa a `OCUPADA`. Si el pedido cambia de mesa,
  la anterior queda `LIBRE`. Al pasar a `ENTREGADO` o `CANCELADO` la mesa queda `LIBRE`.
- Flujo: `PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO`, y `CANCELADO` desde cualquier estado no final.
  Cocina solo pasa a `EN_PREPARACION` y `LISTO`; el mesero solo a `ENTREGADO` y `CANCELADO`.
- Solo se agregan productos disponibles; el precio se copia del producto y el subtotal (`precio × cantidad`) lo calcula el servidor.
- Los productos y observaciones solo se modifican si el pedido está `PENDIENTE` o `EN_PREPARACION`, y un mesero solo toca sus pedidos.
- Solo se eliminan pedidos `PENDIENTE` o `CANCELADO` (se borran también sus productos e historial).
- El estado de un pedido se cambia únicamente con `/estado`; un PUT/PATCH normal no lo modifica.

## Cómo probar (Postman)
Importa `postman/Restaurante.postman_collection.json`. El orden de las carpetas es el del flujo:
1. **RF-01** → *Login mesero* (guarda el token solo).
2. **RF-02** → *Crear pedido* (guarda `idPedido` solo).
3. **RF-03** → *Asignar el pedido a otra mesa*.
4. **RF-04** → *Agregar producto al pedido* (guarda `idDetalle` solo).
5. **RF-05** → *Registrar o cambiar la observación*.
6. *Login cocina* → pasar a `EN_PREPARACION` y `LISTO`; *Login mesero* → `ENTREGADO`.

En la consola, entra como `mesero` y usa las opciones 1 a 4 del menú.

## Notas
- Las claves de `Semilla` y la clave de MySQL de `persistence.xml` son de prueba: cámbialas antes de usar el sistema de verdad.
- En el menú de Administración (CRUD genérico de consola) las reglas de negocio de pedidos y productos del pedido no se aplican:
  es el acceso directo a las tablas. Para trabajar con las reglas usa el menú de mesero o la API.
