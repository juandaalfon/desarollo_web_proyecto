package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Categoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Categoria:  GET /getCategorias  ·  POST /insertCategoria  ·  PUT /updateCategoria  ·  DELETE /deleteCategoria/{id} */
@RestController
@CrossOrigin
public class CategoriaDirectoController {

    private final CrudDirecto<Categoria> crud;

    public CategoriaDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Categoria.class, "idCategoria", mapper);
    }

    @GetMapping("/getCategorias")
    public List<Categoria> getCategorias() {
        return crud.listar();
    }

    @PostMapping("/insertCategoria")
    public ResponseEntity<Map<String, Object>> insertCategoria(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateCategoria")
    public ResponseEntity<Map<String, Object>> updateCategoria(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteCategoria/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategoria(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
