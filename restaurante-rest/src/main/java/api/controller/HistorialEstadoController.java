package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.HistorialEstado;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial-estados")
public class HistorialEstadoController extends CrudController<HistorialEstado> {

    public HistorialEstadoController(ObjectMapper mapper) {
        super(HistorialEstado.class, mapper);
    }
}
