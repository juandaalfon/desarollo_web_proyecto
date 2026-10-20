package api.controller;

import api.security.AuthInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import jakarta.persistence.Id;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import security.Sesion;
import service.ServicioException;

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
 *
 * Los errores se lanzan como ServicioException y los convierte ManejadorErrores en la respuesta JSON.
 * Las tablas con reglas de negocio (pedido, detalle_pedido) sobreescriben los métodos "procesar..." y
 * "listarEntidades" para pasar por su servicio en vez de ir directo al DAO.
 */
@CrossOrigin // permite que el frontend (otro puerto) consuma la API
public abstract class CrudController<T> {

    private final Class<T> clase;
    private final GenericDAO<T> dao;
    private final ObjectMapper mapper;
    private final Field campoId;

    protected CrudController(Class<T> clase, ObjectMapper mapper) {
        this.clase = clase;
        this.dao = GenericDAO.para(clase);
        this.mapper = mapper;
        this.campoId = buscarCampoId(clase);
    }

    @GetMapping
    public List<T> listar(@RequestParam Map<String, String> filtros) {
        return listarEntidades(filtros);
    }

    @GetMapping("/{id}")
    public T buscar(@PathVariable Integer id) {
        return existente(id);
    }

    @PostMapping
    public ResponseEntity<T> crear(@RequestBody T entidad, HttpServletRequest peticion) {
        asignarId(entidad, null); // el id lo genera la BD
        T creada = procesarCreacion(entidad, sesion(peticion));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public T reemplazar(@PathVariable Integer id, @RequestBody T entidad, HttpServletRequest peticion) {
        existente(id);
        asignarId(entidad, id);
        return procesarActualizacion(entidad, sesion(peticion));
    }

    @PatchMapping("/{id}")
    public T modificar(@PathVariable Integer id, @RequestBody JsonNode cambios, HttpServletRequest peticion) {
        T actual = existente(id);
        try {
            // Aplica sobre la entidad actual solo los campos presentes en el JSON
            mapper.readerForUpdating(actual).readValue(cambios);
        } catch (Exception e) {
            throw ServicioException.invalida("JSON inválido: " + e.getMessage());
        }
        asignarId(actual, id);
        return procesarActualizacion(actual, sesion(peticion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpServletRequest peticion) {
        existente(id);
        procesarEliminacion(id, sesion(peticion));
        return ResponseEntity.noContent().build();
    }

    protected List<T> listarEntidades(Map<String, String> filtros) {
        return dao.listar();
    }

    protected T procesarCreacion(T entidad, Sesion sesion) {
        if (!dao.crear(entidad)) {
            throw ServicioException.invalida("No se pudo crear " + clase.getSimpleName()
                    + ": revisa los campos obligatorios, los valores únicos y que las llaves foráneas existan.");
        }
        // Se vuelve a leer para devolver también los datos completos de las relaciones
        T creada = dao.buscarPorId(leerId(entidad));
        return creada != null ? creada : entidad;
    }

    protected T procesarActualizacion(T entidad, Sesion sesion) {
        if (!dao.actualizar(entidad)) {
            throw ServicioException.invalida("No se pudo actualizar " + clase.getSimpleName()
                    + ": revisa los campos obligatorios y que las llaves foráneas existan.");
        }
        return dao.buscarPorId(leerId(entidad));
    }

    protected void procesarEliminacion(Integer id, Sesion sesion) {
        if (!dao.eliminar(id)) {
            throw ServicioException.conflicto("No se puede eliminar: otro registro depende de este (llave foránea).");
        }
    }

    protected static Sesion sesion(HttpServletRequest peticion) {
        return (Sesion) peticion.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
    }

    private T existente(Integer id) {
        T entidad = dao.buscarPorId(id);
        if (entidad == null) {
            throw ServicioException.noEncontrado(clase.getSimpleName() + " con id " + id + " no existe.");
        }
        return entidad;
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
