package api.controller;

import api.service.MesaService;
import model.Mesa;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
public class MesaController extends CrudController<Mesa> {

    public MesaController(MesaService servicio) {
        super(servicio);
    }
}
