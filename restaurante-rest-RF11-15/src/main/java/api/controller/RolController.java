package api.controller;

import api.service.RolService;
import model.Rol;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolController extends CrudController<Rol> {

    public RolController(RolService servicio) {
        super(servicio);
    }
}
