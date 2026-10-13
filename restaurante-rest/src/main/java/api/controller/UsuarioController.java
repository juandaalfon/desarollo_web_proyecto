package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Usuario;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends CrudController<Usuario> {

    public UsuarioController(ObjectMapper mapper) {
        super(Usuario.class, mapper);
    }
}
