package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Producto;
import org.springframework.stereotype.Service;

@Service
public class ProductoService extends CrudService<Producto> {

    public ProductoService(ObjectMapper mapper) {
        super(Producto.class, mapper);
    }
}
