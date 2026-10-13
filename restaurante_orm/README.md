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
