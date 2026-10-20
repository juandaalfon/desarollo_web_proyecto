package api.service;

import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import api.exception.RecursoNoEncontradoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import jakarta.persistence.Id;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Capa de SERVICIO genérica: aquí vive la lógica (validar que exista, armar el resultado, decidir el error)
 * y es la única que habla con el GenericDAO. El controlador solo recibe HTTP y llama al servicio.
 *
 *   Controlador  ->  Servicio (esta clase)  ->  GenericDAO  ->  Hibernate  ->  MySQL
 */
public abstract class CrudService<T> {

    protected final Class<T> clase;
    protected final GenericDAO<T> dao;
    private final ObjectMapper mapper;
    private final Field campoId;

    protected CrudService(Class<T> clase, ObjectMapper mapper) {
        this.clase = clase;
        this.dao = new GenericDAO<>(clase);
        this.mapper = mapper;
        this.campoId = buscarCampoId(clase);
    }

    public List<T> listar() {
        return dao.listar();
    }

    public T buscar(Integer id) {
        T entidad = dao.buscarPorId(id);
        if (entidad == null) {
            throw new RecursoNoEncontradoException(clase.getSimpleName() + " con id " + id + " no existe.");
        }
        return entidad;
    }

    public T crear(T entidad) {
        asignarId(entidad, null); // el id lo genera la BD
        if (!dao.crear(entidad)) {
            throw new OperacionInvalidaException("No se pudo crear " + clase.getSimpleName()
                    + ": revisa los campos obligatorios y que las llaves foráneas existan.");
        }
        // Se vuelve a leer para devolver también los datos completos de las relaciones
        T creada = dao.buscarPorId(leerId(entidad));
        return creada != null ? creada : entidad;
    }

    /** PUT: reemplaza todos los campos. */
    public T reemplazar(Integer id, T entidad) {
        buscar(id); // lanza 404 si no existe
        asignarId(entidad, id);
        if (!dao.actualizar(entidad)) {
            throw new OperacionInvalidaException("No se pudo actualizar " + clase.getSimpleName() + ".");
        }
        return buscar(id);
    }

    /** PATCH: aplica solo los campos presentes en el JSON sobre el registro actual. */
    public T modificar(Integer id, JsonNode cambios) {
        T existente = buscar(id);
        try {
            mapper.readerForUpdating(existente).readValue(cambios);
        } catch (Exception e) {
            throw new OperacionInvalidaException("JSON inválido: " + e.getMessage());
        }
        asignarId(existente, id);
        if (!dao.actualizar(existente)) {
            throw new OperacionInvalidaException("No se pudo actualizar " + clase.getSimpleName() + ".");
        }
        return buscar(id);
    }

    public void eliminar(Integer id) {
        buscar(id); // lanza 404 si no existe
        if (!dao.eliminar(id)) {
            throw new ConflictoException("No se puede eliminar: otro registro depende de este (llave foránea).");
        }
    }

    // ---------- utilidades ----------

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
