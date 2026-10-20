# Restaurante – CRUD con ORM (Hibernate / JPA)

Acceso a datos con **Hibernate** (implementación de JPA) sobre MySQL, para las 9 tablas del diagrama del restaurante,
mediante un único `GenericDAO<T>` y un menú de consola genérico (`ConsolaCrud`).

## Estructura
```
src/main/java
  model/      entidades JPA (@Entity, @ManyToOne = llaves foráneas del diagrama)
  dao/        GenericDAO<T>: crear, buscarPorId, listar, actualizar, eliminar
  util/       JPAUtil: EntityManagerFactory (reemplaza a ConexionBD)
  main/       Main (elige tabla) y ConsolaCrud (menú CRUD por reflexión)
src/main/resources/META-INF/persistence.xml   conexión y entidades
sql/restaurante.sql                            DDL de referencia (Hibernate crea las tablas solo)
```

## Cómo ejecutar
1. Requisitos: JDK 17+ y MySQL en marcha (Maven lo resuelve VS Code con el Extension Pack for Java).
2. Pon tu usuario y clave en `persistence.xml` (`jdbc.user` / `jdbc.password`) **o** define las variables de entorno
   `DB_URL`, `DB_USER`, `DB_PASSWORD`. La base `restaurante` se crea sola.
3. Ejecuta `main.Main` con **Run Java** (o `mvn compile exec:java`).

Orden sugerido para probar (por las llaves foráneas): Rol → Usuario → Mesa → Categoría → Producto → Pedido →
Detalle de pedido → Historial de estado → Factura. Las llaves foráneas se piden como **id** del registro relacionado.
Las fechas se escriben `2026-10-12 19:00`; los booleanos como `s` / `n`.

## Notas sobre el diagrama
- Montos (`precio`, `precio_unitario`, `subtotal`, `total`) → `DECIMAL`/`BigDecimal`.
- `usuario.contrasena_hash` se guarda **tal como se envía**: el código no calcula el hash. Para producción conviene guardar
  un hash BCrypt, no la clave en texto.
- Las relaciones se leyeron del diagrama; la parte derecha de la imagen venía cortada, por lo que `factura → pedido`
  ("genera") y `detalle_pedido → pedido` ("contiene") se dedujeron por las llaves `id_pedido` de cada tabla.
- `pedido.fecha_ultima_actualizacion`, `producto.descripcion` y `detalle_pedido.notas` son opcionales.


---
# API REST (capa nueva)

Arquitectura: `Postman → Controlador → Servicio → GenericDAO → Hibernate → MySQL`.

Sobre el mismo ORM se agregó una API REST con Spring Boot (solo aporta el servidor web y el JSON; el acceso a datos
sigue siendo `GenericDAO` + Hibernate).

```
src/main/java/api/RestauranteApplication.java      arranca el servidor (puerto 8080)
src/main/java/api/controller/CrudController.java   controlador genérico: recibe HTTP (GET, POST, PUT, PATCH, DELETE)
src/main/java/api/controller/*Controller.java      uno por tabla (solo indica la ruta y su servicio)
src/main/java/api/service/CrudService.java         servicio genérico: lógica y llamadas al GenericDAO
src/main/java/api/service/*Service.java            uno por tabla
src/main/java/api/exception/                       404 / 400 / 409 convertidos a JSON (ManejadorErrores)
postman/PoliRestaurante.postman_collection.json    colección lista para importar en Postman
```

## Ejecutar
`mvn spring-boot:run`  (o ejecutar `api.RestauranteApplication` con Run Java). Debe haber MySQL encendido.
La consola CRUD anterior sigue disponible con `mvn compile exec:java`.

## Endpoints (mismos 5 verbos para cada ruta)
| Verbo | Ruta | Acción | Respuesta |
|---|---|---|---|
| GET | `/api/{recurso}` | listar todos | 200 |
| GET | `/api/{recurso}/{id}` | buscar uno | 200 / 404 |
| POST | `/api/{recurso}` | crear | 201 / 400 |
| PUT | `/api/{recurso}/{id}` | reemplazar todo | 200 / 404 / 400 |
| PATCH | `/api/{recurso}/{id}` | cambiar solo los campos enviados | 200 / 404 / 400 |
| DELETE | `/api/{recurso}/{id}` | eliminar | 204 / 404 / 409 |

Recursos: `roles`, `usuarios`, `mesas`, `categorias`, `productos`, `pedidos`, `detalles-pedido`, `historial-estados`, `facturas`.

Las llaves foráneas se envían como objeto con su id: `"mesa": {"idMesa": 1}`. Las fechas van en formato
`2026-10-12T19:00:00`. `contrasenaHash` se recibe pero nunca se devuelve en las respuestas.
Orden para probar: roles → usuarios → mesas → categorías → productos → pedidos → detalles → historial → facturas.

---
# Requerimientos funcionales RF-06 a RF-10

Las acciones del flujo, la cocina y los reportes llevan el encabezado **`X-Usuario-Id`** con el id del usuario que
realiza la acción (el proyecto aún no tiene login; cuando se agregue, solo cambia `AccesoService`).
El rol se reconoce por el nombre del rol en la tabla `rol`: debe contener *Mesero*, *Cocina* o *Admin*.

| RF | Qué hace | Endpoint | Quién |
|---|---|---|---|
| RF-06 | Cola de pedidos PENDIENTES (más antiguo primero, con productos y observaciones) | `GET /api/cocina/pedidos/pendientes` | Cocina, Administrador |
| RF-07 | Cambia el estado respetando el flujo | `PATCH /api/pedidos/{id}/estado` body `{"estado":"EN_PREPARACION"}` | Cocina (EN_PREPARACION, LISTO), Mesero (ENTREGADO, CANCELADO), Administrador (todo) |
| RF-08 | Cancela (solo desde PENDIENTE o EN_PREPARACION) | `PATCH /api/pedidos/{id}/cancelar` | Mesero, Administrador |
| RF-09 | El mesero marca el pedido como entregado (debe estar LISTO) | `PATCH /api/pedidos/{id}/entregar` | Mesero, Administrador |
| RF-10 | Historial automático de cada estado con fecha/hora y duración de cada etapa | `GET /api/pedidos/{id}/historial` y `GET /api/historial-estados` | Todos (solo lectura) |

## Flujo de estados (RF-07, RF-08, RF-09)
```
PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
     \             \
      +-------------+--> CANCELADO   (solo desde PENDIENTE o EN_PREPARACION)
```
- Un pedido nuevo nace **PENDIENTE** y su primer registro del historial se crea solo.
- Cada cambio guarda, en **una sola transacción**, el nuevo estado + `fecha_ultima_actualizacion` + una fila en `historial_estado`
  con la fecha/hora y el usuario que lo hizo.
- El estado **no** se puede cambiar por `PUT`/`PATCH /api/pedidos/{id}`; solo por los endpoints del flujo.
- Un cambio no permitido responde **409**; un rol que no corresponde responde **403**; sin `X-Usuario-Id` responde **400**.
- El historial es de solo lectura: `POST/PUT/PATCH/DELETE /api/historial-estados` responde 403.

## Probar en Postman
Importa `postman/PoliRestaurante.postman_collection.json` y ejecuta la carpeta **A. Flujo demo completo** en orden
(sobre una base de datos vacía; guarda los ids en variables). Luego la carpeta **B** (cancelaciones y errores).
