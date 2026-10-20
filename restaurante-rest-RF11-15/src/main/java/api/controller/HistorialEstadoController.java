package api.controller;

import api.service.HistorialEstadoService;
import model.HistorialEstado;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial-estados")
public class HistorialEstadoController extends CrudController<HistorialEstado> {

    public HistorialEstadoController(HistorialEstadoService servicio) {
        super(servicio);
    }
}
