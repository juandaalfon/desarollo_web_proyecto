package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Categoria;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-04: CRUD de categorías de producto (consulta para todos, cambios solo Administración). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController extends CrudController<Categoria> {

    public CategoriaController(ObjectMapper mapper) {
        super(Categoria.class, mapper);
    }
}
