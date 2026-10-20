package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService extends CrudService<Usuario> {

    public UsuarioService(ObjectMapper mapper) {
        super(Usuario.class, mapper);
    }
}
