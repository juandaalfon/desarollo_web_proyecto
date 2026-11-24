package api.controller;

import api.service.UsuarioService;
import model.Usuario;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends CrudController<Usuario> {

    public UsuarioController(UsuarioService servicio) {
        super(servicio);
    }
}
