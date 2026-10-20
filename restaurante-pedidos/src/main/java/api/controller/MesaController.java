package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mesa;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-03: CRUD de mesas. Todos los roles las consultan; solo Administración las crea, edita o elimina.
 * El estado (LIBRE/OCUPADA) lo cambia automáticamente el servicio de pedidos al asignar o liberar la mesa.
 */
@RestController
@RequestMapping("/api/mesas")
public class MesaController extends CrudController<Mesa> {

    public MesaController(ObjectMapper mapper) {
        super(Mesa.class, mapper);
    }
}
