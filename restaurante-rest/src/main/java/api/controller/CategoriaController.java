package api.controller;

import api.service.CategoriaService;
import model.Categoria;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController extends CrudController<Categoria> {

    public CategoriaController(CategoriaService servicio) {
        super(servicio);
    }
}
