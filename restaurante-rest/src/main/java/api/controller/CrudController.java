package api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import jakarta.persistence.Id;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST genérico: cada método HTTP se traduce a una operación del GenericDAO.
 *
 *   GET    /api/xxx        -> listar()
 *   GET    /api/xxx/{id}   -> buscarPorId()
 *   POST   /api/xxx        -> crear()
 *   PUT    /api/xxx/{id}   -> actualizar()  (reemplaza todos los campos)
 *   PATCH  /api/xxx/{id}   -> actualizar()  (cambia solo los campos enviados)
 *   DELETE /api/xxx/{id}   -> eliminar()
 *
 * Las llaves foráneas se envían como objeto con su id, p. ej.  "mesa": {"idMesa": 1}
 */
@CrossOrigin // permite que el frontend (otro puerto) consuma la API
public abstract class CrudController<T> {

    private final Class<T> clase;
    private final GenericDAO<T> dao;
    private final ObjectMapper mapper;
    private final Field campoId;

    protected CrudController(Class<T> clase, ObjectMapper mapper) {
        this.clase = clase;
        this.dao = new GenericDAO<>(clase);
        this.mapper = mapper;
        this.campoId = buscarCampoId(clase);
    }

    @GetMapping
    public List<T> listar() {
        return dao.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Integer id) {
        T entidad = dao.buscarPorId(id);
        if (entidad == null) {
            return noEncontrado(id);
        }
        return ResponseEntity.ok(entidad);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody T entidad) {
        asignarId(entidad, null); // el id lo genera la BD
        if (!dao.crear(entidad)) {
            return error(HttpStatus.BAD_REQUEST,
                    "No se pudo crear " + clase.getSimpleName() + ": revisa los campos obligatorios y que las llaves foráneas existan.");
        }
        // Se vuelve a leer para devolver también los datos completos de las relaciones
        T creada = dao.buscarPorId(leerId(entidad));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada != null ? creada : entidad);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> reemplazar(@PathVariable Integer id, @RequestBody T entidad) {
        if (dao.buscarPorId(id) == null) {
            return noEncontrado(id);
        }
        asignarId(entidad, id);
        if (!dao.actualizar(entidad)) {
            return error(HttpStatus.BAD_REQUEST, "No se pudo actualizar " + clase.getSimpleName() + ".");
        }
        return ResponseEntity.ok(dao.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Integer id, @RequestBody JsonNode cambios) {
        T existente = dao.buscarPorId(id);
        if (existente == null) {
            return noEncontrado(id);
        }
        try {
            // Aplica sobre la entidad actual solo los campos presentes en el JSON
            mapper.readerForUpdating(existente).readValue(cambios);
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "JSON inválido: " + e.getMessage());
        }
        asignarId(existente, id);
        if (!dao.actualizar(existente)) {
            return error(HttpStatus.BAD_REQUEST, "No se pudo actualizar " + clase.getSimpleName() + ".");
        }
        return ResponseEntity.ok(dao.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        if (dao.buscarPorId(id) == null) {
            return noEncontrado(id);
        }
        if (!dao.eliminar(id)) {
            return error(HttpStatus.CONFLICT,
                    "No se puede eliminar: otro registro depende de este (llave foránea).");
        }
        return ResponseEntity.noContent().build();
    }

    // ---------- utilidades ----------

    private ResponseEntity<Map<String, Object>> noEncontrado(Integer id) {
        return error(HttpStatus.NOT_FOUND, clase.getSimpleName() + " con id " + id + " no existe.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("status", estado.value(), "error", mensaje));
    }

    private static Field buscarCampoId(Class<?> clase) {
        for (Field f : clase.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalStateException(clase.getSimpleName() + " no tiene un campo @Id");
    }

    private void asignarId(T entidad, Integer id) {
        try {
            campoId.set(entidad, id);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private Integer leerId(T entidad) {
        try {
            return (Integer) campoId.get(entidad);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
