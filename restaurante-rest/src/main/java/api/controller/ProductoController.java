package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Producto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController extends CrudController<Producto> {

    public ProductoController(ObjectMapper mapper) {
        super(Producto.class, mapper);
    }
}
