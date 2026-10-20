package api.controller;

import api.service.CrudService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST genérico. Solo traduce HTTP -> llamada al servicio:
 *
 *   GET    /api/xxx        -> servicio.listar()
 *   GET    /api/xxx/{id}   -> servicio.buscar()
 *   POST   /api/xxx        -> servicio.crear()
 *   PUT    /api/xxx/{id}   -> servicio.reemplazar()  (reemplaza todos los campos)
 *   PATCH  /api/xxx/{id}   -> servicio.modificar()   (cambia solo los campos enviados)
 *   DELETE /api/xxx/{id}   -> servicio.eliminar()
 *
 * Los errores (404, 400, 409) los lanza el servicio como excepciones y los convierte ManejadorErrores.
 * Las llaves foráneas se envían como objeto con su id, p. ej.  "mesa": {"idMesa": 1}
 */
@CrossOrigin // permite que el frontend (otro puerto) consuma la API
public abstract class CrudController<T> {

    private final CrudService<T> servicio;

    protected CrudController(CrudService<T> servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<T> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public T buscar(@PathVariable Integer id) {
        return servicio.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T crear(@RequestBody T entidad) {
        return servicio.crear(entidad);
    }

    @PutMapping("/{id}")
    public T reemplazar(@PathVariable Integer id, @RequestBody T entidad) {
        return servicio.reemplazar(id, entidad);
    }

    @PatchMapping("/{id}")
    public T modificar(@PathVariable Integer id, @RequestBody JsonNode cambios) {
        return servicio.modificar(id, cambios);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
