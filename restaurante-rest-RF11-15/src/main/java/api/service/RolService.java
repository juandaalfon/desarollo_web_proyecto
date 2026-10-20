package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Rol;
import org.springframework.stereotype.Service;

@Service
public class RolService extends CrudService<Rol> {

    public RolService(ObjectMapper mapper) {
        super(Rol.class, mapper);
    }
}
