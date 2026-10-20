package api.controller;

import api.service.ProductoService;
import model.Producto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController extends CrudController<Producto> {

    public ProductoController(ProductoService servicio) {
        super(servicio);
    }
}
