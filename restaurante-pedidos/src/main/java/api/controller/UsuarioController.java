package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Usuario;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-01: CRUD de usuarios (solo Administración).
 * La contraseña se envía en el campo "contrasenaHash" y UsuarioDAO la cifra con BCrypt antes de guardarla.
 * Nunca se devuelve en las respuestas.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends CrudController<Usuario> {

    public UsuarioController(ObjectMapper mapper) {
        super(Usuario.class, mapper);
    }
}
