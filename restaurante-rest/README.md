# Restaurante – CRUD con ORM (Hibernate / JPA)

Acceso a datos con **Hibernate** (implementación de JPA) sobre MySQL, para las 9 tablas del diagrama del restaurante,
mediante un único `GenericDAO<T>` y un menú de consola genérico (`ConsolaCrud`). Sobre eso se agregó la API REST, el login por rol
y las reglas de negocio (RF-01 a RF-15, más abajo).

## Estructura
```
src/main/java
  model/      entidades JPA (@Entity, @ManyToOne = llaves foráneas del diagrama)
  dao/        GenericDAO<T>: crear, buscarPorId, listar, actualizar, eliminar
  util/       JPAUtil: EntityManagerFactory (reemplaza a ConexionBD)
  main/       Main (login), menús por rol y ConsolaCrud (menú CRUD por reflexión)
src/main/resources/META-INF/persistence.xml   conexión y entidades
sql/restaurante.sql                            DDL de referencia (Hibernate crea las tablas solo)
```

## Cómo ejecutar
1. Requisitos: JDK 17+ y MySQL en marcha (Maven lo resuelve VS Code con el Extension Pack for Java).
2. Pon tu usuario y clave en `persistence.xml` (`jdbc.user` / `jdbc.password`) **o** define las variables de entorno
   `DB_URL`, `DB_USER`, `DB_PASSWORD`. La base `restaurante` se crea sola.
3. Ejecuta `main.Main` con **Run Java** (o `mvn compile exec:java`) e inicia sesión (usuarios de prueba más abajo).

Orden sugerido para probar (por las llaves foráneas): Rol → Usuario → Mesa → Categoría → Producto → Pedido →
Detalle de pedido → Historial de estado → Factura. Las llaves foráneas se piden como **id** del registro relacionado.
Las fechas se escriben `2026-10-12 19:00`; los booleanos como `s` / `n`.

## Notas sobre el diagrama
- Montos (`precio`, `precio_unitario`, `subtotal`, `total`) → `DECIMAL`/`BigDecimal`.
- `usuario.contrasena_hash` se guarda como hash **BCrypt**: `UsuarioDAO` (consola) y `UsuarioService` (API) cifran la clave antes de guardarla.
- Las relaciones se leyeron del diagrama; la parte derecha de la imagen venía cortada, por lo que `factura → pedido`
  ("genera") y `detalle_pedido → pedido` ("contiene") se dedujeron por las llaves `id_pedido` de cada tabla.
- `pedido.fecha_ultima_actualizacion`, `producto.descripcion` y `detalle_pedido.notas` son opcionales.

---
# API REST, login y reglas de negocio (RF-01 a RF-15)

Arquitectura: `Postman → Controlador → Servicio → DAO → Hibernate → MySQL`, con un filtro de seguridad
(`AuthInterceptor`) delante de los controladores.

Spring Boot solo aporta el servidor web y el JSON; el acceso a datos sigue siendo `GenericDAO` + Hibernate.

```
src/main/java
  model/                 entidades JPA (9 tablas) y enums EstadoPedido / EstadoMesa
  dao/                   GenericDAO, UsuarioDAO (cifra la clave con BCrypt), PedidoDAO, DetallePedidoDAO, ReporteDAO
  util/                  JPAUtil (EntityManagerFactory + enLectura/enTransaccion), Texto, Semilla (datos iniciales)
  security/              Sesion (quién inició sesión) y Permisos (qué rol usa qué ruta)               <- RF-01
  api/RestauranteApplication.java     arranca el servidor (puerto 8080) y carga la Semilla
  api/security/          AuthInterceptor (exige token y rol) y WebConfig                              <- RF-01
  api/controller/        CrudController genérico + un controlador por tabla + AuthController, CocinaController, ReporteController
  api/service/           CrudService genérico + un servicio por tabla (reglas de negocio) + AuthService, AccesoService
  api/dto/ api/exception/  respuestas especiales y errores (401 / 403 / 404 / 400 / 409) convertidos a JSON
  main/                  consola: Main (login), MenuAdmin, MenuMesero, MenuCocina, ConsolaCrud, Entrada, Vista
src/main/resources/META-INF/persistence.xml
sql/restaurante.sql
postman/Restaurante.postman_collection.json      colección lista para importar (RF-01 a RF-15)
```

## Ejecutar
1. JDK 17+, Maven y MySQL en marcha. Usuario y clave de MySQL en `persistence.xml` o en `DB_URL`, `DB_USER`, `DB_PASSWORD`.
2. **API REST:** `mvn spring-boot:run` (o ejecutar `api.RestauranteApplication`) → `http://localhost:8080`
3. **Consola con teclado:** `mvn compile exec:java` (o ejecutar `main.Main`): pide usuario y contraseña y abre el menú de su rol.

La primera vez la `Semilla` crea los roles, estos usuarios, 5 mesas, 2 categorías y 4 productos:

| Usuario | Contraseña | Rol |
|---|---|---|
| admin | admin123 | Administrador |
| mesero | mesero123 | Mesero |
| cocina | cocina123 | Cocina |

Las claves de la `Semilla` y la de MySQL son de prueba: cámbialas antes de usar el sistema de verdad.
El rol se reconoce por el nombre del rol en la tabla `rol`: debe contener *Mesero*, *Cocina* o *Admin*.

## Requerimientos

| RF | Qué hace | Endpoint | Quién |
|---|---|---|---|
| RF-01 | Login con usuario y contraseña (BCrypt); token de sesión de 8 horas; acceso según el rol | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` | Todos |
| RF-02 | El mesero crea pedidos (nace PENDIENTE y queda en el historial) | `POST /api/pedidos` `{"mesa":{"idMesa":1},"numPersonas":2}` · `GET /api/pedidos?estado=PENDIENTE` | Mesero, Administrador |
| RF-03 | Asignación de mesa (debe estar LIBRE y alcanzar la capacidad; pasa a OCUPADA) | `PUT /api/pedidos/{id}/mesa` `{"idMesa":3}` | Mesero, Administrador |
| RF-04 | Productos del pedido (precio y subtotal los calcula el servidor) | `POST /api/detalles-pedido` · `GET /api/detalles-pedido?idPedido=1` | Mesero, Administrador |
| RF-05 | Observaciones de cada producto | `PATCH /api/detalles-pedido/{id}/notas` `{"notas":"Sin picante"}` (o `notas` al agregar el producto) | Mesero, Administrador |
| RF-06 | Cola de pedidos PENDIENTES (más antiguo primero, con productos y observaciones) | `GET /api/cocina/pedidos/pendientes` | Cocina, Administrador |
| RF-07 | Cambia el estado respetando el flujo | `PATCH /api/pedidos/{id}/estado` body `{"estado":"EN_PREPARACION"}` | Cocina (EN_PREPARACION, LISTO), Mesero (ENTREGADO, CANCELADO), Administrador (todo) |
| RF-08 | Cancela (solo desde PENDIENTE o EN_PREPARACION) | `PATCH /api/pedidos/{id}/cancelar` | Mesero, Administrador |
| RF-09 | El mesero marca el pedido como entregado (debe estar LISTO) | `PATCH /api/pedidos/{id}/entregar` | Mesero, Administrador |
| RF-10 | Historial automático de cada estado con fecha/hora y duración de cada etapa | `GET /api/pedidos/{id}/historial` y `GET /api/historial-estados?idPedido=5` | Todos (solo lectura) |
| RF-11 | Menú: productos, precios y categorías con validaciones | `/api/productos`, `/api/categorias` (GET, POST, PUT, PATCH, DELETE) | Administrador (los demás consultan) |
| RF-12 | Disponibilidad de productos | `PATCH /api/productos/{id}/disponibilidad` body `{"disponible":false}`; `GET /api/productos/disponibles` | Administrador |
| RF-13 | Mesas y su estado LIBRE / RESERVADA / OCUPADA | `/api/mesas` y `PATCH /api/mesas/{id}/estado` body `{"estado":"OCUPADA"}` | Administrador |
| RF-14 | Usuarios (login único, rol válido, contraseña guardada con hash BCrypt) | `/api/usuarios`, `/api/roles` | Administrador |
| RF-15 | Reportes de ventas | `GET /api/reportes/ventas`, `/productos-mas-vendidos`, `/tiempos-preparacion` (opcional `?desde=2026-10-01&hasta=2026-10-31`) | Administrador |

Además, cada tabla tiene los 6 verbos (`/api/roles`, `/usuarios`, `/mesas`, `/categorias`, `/productos`, `/pedidos`,
`/detalles-pedido`, `/historial-estados`, `/facturas`):

| Verbo | Ruta | Acción | Respuesta |
|---|---|---|---|
| GET | `/api/{recurso}` | listar todos | 200 |
| GET | `/api/{recurso}/{id}` | buscar uno | 200 / 404 |
| POST | `/api/{recurso}` | crear | 201 / 400 |
| PUT | `/api/{recurso}/{id}` | reemplazar todo | 200 / 404 / 400 |
| PATCH | `/api/{recurso}/{id}` | cambiar solo los campos enviados | 200 / 404 / 400 |
| DELETE | `/api/{recurso}/{id}` | eliminar | 204 / 404 / 409 |

Las llaves foráneas se envían como objeto con su id: `"mesa": {"idMesa": 1}`. Las fechas van como `2026-10-12T19:00:00`.
Al crear un usuario, la contraseña va en `contrasenaHash` (el sistema la cifra; nunca se devuelve).

## RF-01: login y permisos por rol
`POST /api/auth/login` con `{"usuarioLogin": "mesero", "contrasena": "mesero123"}` devuelve un `token`.
Todas las demás peticiones llevan el header `Authorization: Bearer <token>` (ya no se usa `X-Usuario-Id`).
Sin token → **401**. Con un rol sin permiso → **403**. Las sesiones viven en memoria: al reiniciar el servidor hay que iniciar sesión otra vez.

| Recurso | Administrador | Mesero | Cocina |
|---|---|---|---|
| roles, usuarios, facturas, reportes | todo | – | – |
| mesas, categorías, productos | todo | consultar | consultar |
| historial-estados | consultar | consultar | consultar |
| pedidos | todo | consultar y gestionar los suyos | consultar y pasar a EN_PREPARACION / LISTO |
| detalles-pedido | todo | consultar y gestionar los de sus pedidos | consultar |
| cocina (RF-06) | todo | – | todo |

## Flujo de estados (RF-07, RF-08, RF-09)
```
PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
     \             \
      +-------------+--> CANCELADO   (solo desde PENDIENTE o EN_PREPARACION)
```
- Un pedido nuevo nace **PENDIENTE**, a nombre del mesero con sesión iniciada, y su primer registro del historial se crea solo.
  Si lo crea un administrador, debe indicar `"mesero": {"idUsuario": 1}` en el body.
- Cada cambio guarda, en **una sola transacción**, el nuevo estado + `fecha_ultima_actualizacion` + una fila en `historial_estado`
  con la fecha/hora y el usuario que lo hizo.
- No se puede pasar a EN_PREPARACION un pedido sin productos.
- El estado **no** se puede cambiar por `PUT`/`PATCH /api/pedidos/{id}`; solo por los endpoints del flujo. Esos dos verbos solo
  modifican la mesa y el número de personas.
- Un cambio no permitido responde **409**; un rol que no corresponde o un pedido de otro mesero responde **403**; sin token responde **401**.
- El historial es de solo lectura: `POST/PUT/PATCH/DELETE /api/historial-estados` responde 403.

## Reglas de datos
- **Mesas (RF-03, RF-13):** número y capacidad mayores que 0, número sin repetir; estado por defecto `LIBRE`.
  Al crear un pedido o asignarlo a una mesa, la mesa debe estar `LIBRE` y tener capacidad para las personas; pasa a `OCUPADA`.
  Si el pedido cambia de mesa, la anterior queda `LIBRE`. Al pasar a `ENTREGADO` o `CANCELADO` la mesa queda `LIBRE`.
- **Productos del pedido (RF-04, RF-05):** solo se agregan productos disponibles; el precio se copia del producto y el subtotal
  (`precio × cantidad`) lo calcula el servidor. Los productos y observaciones solo se modifican si el pedido está `PENDIENTE` o
  `EN_PREPARACION`, y un mesero solo toca sus pedidos. La observación admite máximo 255 caracteres (con `""` se borra).
- **Eliminar pedidos:** solo `PENDIENTE` o `CANCELADO` (se borran también sus productos e historial).
- **Producto (RF-11, RF-12):** nombre obligatorio, precio mayor que 0, categoría existente; `disponible` por defecto `true`.
- **Categoría:** nombre obligatorio y sin repetir.
- **Usuario (RF-14):** login sin repetir, rol existente, contraseña de mínimo 4 caracteres (se guarda como hash BCrypt y nunca se devuelve).

## Reportes (RF-15)
Una venta es un pedido **ENTREGADO** (los cancelados no cuentan); su fecha es la del cambio a ENTREGADO en el historial.
Sin `desde`/`hasta` se usan los últimos 30 días.
- `/ventas`: total vendido, número de pedidos, ticket promedio, ventas por día y por categoría.
- `/productos-mas-vendidos?limite=10`: unidades y total por producto.
- `/tiempos-preparacion`: tiempo promedio en cada etapa y tiempo total promedio.

## Consola con teclado
`main.Main` pide usuario y contraseña y abre el menú del rol:
- **Administrador:** CRUD de las 9 tablas (`ConsolaCrud`). Es acceso directo a las tablas: las reglas de pedidos y productos del pedido no se aplican ahí.
- **Mesero:** crear pedido (RF-02), cambiar de mesa (RF-03), agregar producto (RF-04), observación (RF-05), quitar producto, ver pedidos, entregar o cancelar (RF-09 / RF-08).
- **Cocina:** cola de pendientes (RF-06), pedidos activos y cambio de estado (RF-07).

El mesero y la cocina usan los mismos servicios que la API, así que las reglas son las mismas.

## Probar en Postman
Importa `postman/Restaurante.postman_collection.json`.
1. Carpetas **RF-01 a RF-05**: *Login* guarda el token solo; luego crear pedido, asignar mesa, agregar producto y observación (el orden de las carpetas es el del flujo).
2. Carpeta **A. Flujo demo completo** (RF-06 a RF-15): inicia sesión con los tres usuarios de la Semilla, guarda los tokens e ids en variables y recorre el flujo hasta los reportes.
3. Carpetas **B** (cancelaciones y errores), **C** (disponibilidad y mesas) y **9 Facturas**.

---
# Capa de presentación (interfaz web)

Interfaz web en HTML, CSS y JavaScript puro (sin dependencias ni paso de compilación). Spring Boot la sirve desde
`src/main/resources/static`, así que comparte origen con la API: no hay CORS ni configuración extra.

1. Arranca la API: `mvn spring-boot:run`
2. Abre **http://localhost:8080** e inicia sesión (admin / mesero / cocina, ver tabla de usuarios de prueba).

```
src/main/resources/static
  index.html            página única (SPA con rutas por hash: #/pedidos, #/cocina...)
  css/styles.css        estilos (tema claro/oscuro automático, responsive)
  js/api.js             cliente HTTP: guarda el token (RF-01) y lo envía en Authorization: Bearer
  js/ui.js              utilidades: nodos DOM, formato, avisos y diálogos
  js/app.js             login, navegación según el rol y enrutador
  js/views/             pedidos (lista y detalle), cocina, mesas, resumen (reportes), menu, usuarios, facturas, crud (genérico)
```

| Rol | Qué ve | Requerimientos |
|---|---|---|
| Mesero | Crea pedidos, asigna/cambia mesa, agrega productos y observaciones, entrega o cancela, consulta mesas | RF-02 a RF-05, RF-08, RF-09 |
| Cocina | Tablero con pendientes y en preparación (se actualiza cada 10 s, resalta pedidos con más de 15 min) | RF-06, RF-07 |
| Administrador | Reportes, todos los pedidos con historial, menú y disponibilidad, mesas, usuarios, facturas | RF-10 a RF-15 |

Los permisos reales los sigue aplicando el servidor (`Permisos` / `AuthInterceptor`); la interfaz solo oculta lo que cada rol no puede usar.
El token se guarda en `sessionStorage` (se pierde al cerrar la pestaña) y, si el servidor responde 401, vuelve al login.

---
# CRUD directo estilo `getX / insertX / updateX / deleteX`

Además de la API `/api/**`, hay un conjunto de servicios sencillos con el mismo estilo del servicio de Persona
(`mainService.py`), en `src/main/java/api/controller/directo/`. Hablan directo con `GenericDAO` (CRUD puro, sin reglas de negocio).
Usan el mismo servidor (`mvn spring-boot:run`, puerto 8080) y no necesitan token.

| Tabla | GET | POST | PUT | DELETE |
|---|---|---|---|---|
| rol | `/getRoles` | `/insertRol` | `/updateRol` | `/deleteRol/{id}` |
| usuario | `/getUsuarios` | `/insertUsuario` | `/updateUsuario` | `/deleteUsuario/{id}` |
| mesa | `/getMesas` | `/insertMesa` | `/updateMesa` | `/deleteMesa/{id}` |
| categoria | `/getCategorias` | `/insertCategoria` | `/updateCategoria` | `/deleteCategoria/{id}` |
| producto | `/getProductos` | `/insertProducto` | `/updateProducto` | `/deleteProducto/{id}` |
| pedido | `/getPedidos` | `/insertPedido` | `/updatePedido` | `/deletePedido/{id}` |
| detalle_pedido | `/getDetallesPedido` | `/insertDetallePedido` | `/updateDetallePedido` | `/deleteDetallePedido/{id}` |
| historial_estado | `/getHistorialEstados` | `/insertHistorialEstado` | `/updateHistorialEstado` | `/deleteHistorialEstado/{id}` |
| factura | `/getFacturas` | `/insertFactura` | `/updateFactura` | `/deleteFactura/{id}` |

- **POST**: JSON con los campos; el id lo genera la BD y se devuelve (`{"statusCode":200,"message":"Success","idMesa":6}`).
- **PUT**: el body lleva el id (`idMesa`, `idPedido`...) y solo los campos a cambiar.
- Llaves foráneas como objeto con su id: `"mesa": {"idMesa": 1}`. Fechas: `2026-11-03T14:30:00`.
- Errores: `{"statusCode":400,"err":true,"message":"..."}` (400 datos inválidos, 404 no existe, 409 tiene dependencias).
- La contraseña de `usuario` (`contrasenaHash`) se cifra con BCrypt al guardar y nunca se devuelve.

Ejemplos:
```
POST /insertCategoria  {"nombre": "Postres"}
POST /insertProducto   {"nombre":"Flan","descripcion":"Casero","precio":9000,"categoria":{"idCategoria":3},"disponible":true}
PUT  /updateProducto   {"idProducto":5,"precio":9500}
DELETE /deleteProducto/5
```
> Estas rutas están fuera de `/api/**`, así que **no pasan por el login ni por los permisos por rol**. Úselas para la práctica/desarrollo.
