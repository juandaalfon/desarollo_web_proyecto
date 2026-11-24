package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Lógica común de los controladores "directos" (estilo getX / insertX / updateX / deleteX del
 * servicio de Persona). Habla directamente con el GenericDAO: es un CRUD puro, sin reglas de
 * negocio ni sesión.
 *
 * Respuestas con el mismo formato del ejemplo:
 *   {"statusCode": 200, "message": "Success"}   o   {"statusCode": 400, "err": true, "message": "..."}
 */
public class CrudDirecto<T> {

    private final Class<T> clase;
    private final String nombreId;
    private final GenericDAO<T> dao;
    private final ObjectMapper mapper;
    private final Field campoId;

    /**
     * @param clase    entidad (model.Pedido, model.Mesa...)
     * @param nombreId nombre del atributo llave primaria (idPedido, idMesa...)
     */
    public CrudDirecto(Class<T> clase, String nombreId, ObjectMapper mapper) {
        this.clase = clase;
        this.nombreId = nombreId;
        this.mapper = mapper;
        this.dao = GenericDAO.para(clase); // Usuario usa UsuarioDAO, que cifra la contraseña con BCrypt
        try {
            this.campoId = clase.getDeclaredField(nombreId);
            this.campoId.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(clase.getSimpleName() + " no tiene el campo " + nombreId, e);
        }
    }

    /** GET: todos los registros. */
    public List<T> listar() {
        return dao.listar();
    }

    /** POST: inserta con el JSON del body. El id lo genera la base de datos. */
    public ResponseEntity<Map<String, Object>> insertar(JsonNode cuerpo) {
        try {
            T entidad = mapper.treeToValue(cuerpo, clase);
            asignarId(entidad, null);
            if (!dao.crear(entidad)) {
                return error(400, "No se pudo insertar: revisa los campos obligatorios y que las llaves "
                        + "foráneas existan (se envían como objeto, p. ej. \"mesa\": {\"idMesa\": 1}).");
            }
            Map<String, Object> cuerpoRespuesta = exito();
            cuerpoRespuesta.put(nombreId, leerId(entidad));
            return ResponseEntity.ok(cuerpoRespuesta);
        } catch (Exception e) {
            return error(400, "JSON inválido: " + e.getMessage());
        }
    }

    /** PUT: actualiza el registro cuyo id viene en el body; solo cambia los campos enviados. */
    public ResponseEntity<Map<String, Object>> actualizar(JsonNode cuerpo) {
        try {
            JsonNode nodoId = cuerpo.get(nombreId);
            if (nodoId == null || !nodoId.canConvertToInt()) {
                return error(400, "Falta el campo " + nombreId + " (número entero).");
            }
            Integer id = nodoId.asInt();
            T existente = dao.buscarPorId(id);
            if (existente == null) {
                return error(404, "No existe un registro de " + clase.getSimpleName() + " con " + nombreId + " " + id);
            }
            mapper.readerForUpdating(existente).readValue(cuerpo);
            asignarId(existente, id);
            if (!dao.actualizar(existente)) {
                return error(400, "No se pudo actualizar " + clase.getSimpleName()
                        + ": revisa los valores y las llaves foráneas.");
            }
            return ResponseEntity.ok(exito());
        } catch (Exception e) {
            return error(400, "JSON inválido: " + e.getMessage());
        }
    }

    /** DELETE: elimina por id. */
    public ResponseEntity<Map<String, Object>> eliminar(Integer id) {
        if (dao.buscarPorId(id) == null) {
            return error(404, "No existe un registro de " + clase.getSimpleName() + " con " + nombreId + " " + id);
        }
        if (!dao.eliminar(id)) {
            return error(409, "No se puede eliminar: otro registro depende de este (llave foránea).");
        }
        return ResponseEntity.ok(exito());
    }

    // ---------- utilidades ----------

    private Map<String, Object> exito() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("statusCode", 200);
        m.put("message", "Success");
        return m;
    }

    private ResponseEntity<Map<String, Object>> error(int codigo, String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("statusCode", codigo);
        m.put("err", true);
        m.put("message", mensaje);
        return ResponseEntity.status(HttpStatus.valueOf(codigo)).body(m);
    }

    private void asignarId(T entidad, Integer id) {
        try {
            campoId.set(entidad, id);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object leerId(T entidad) {
        try {
            return campoId.get(entidad);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
