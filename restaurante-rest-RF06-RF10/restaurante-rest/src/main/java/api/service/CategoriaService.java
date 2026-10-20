package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Categoria;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService extends CrudService<Categoria> {

    public CategoriaService(ObjectMapper mapper) {
        super(Categoria.class, mapper);
    }
}
