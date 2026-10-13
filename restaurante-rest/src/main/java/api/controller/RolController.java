package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Rol;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolController extends CrudController<Rol> {

    public RolController(ObjectMapper mapper) {
        super(Rol.class, mapper);
    }
}
