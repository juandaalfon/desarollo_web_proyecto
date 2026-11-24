package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Producto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Producto:  GET /getProductos  ·  POST /insertProducto  ·  PUT /updateProducto  ·  DELETE /deleteProducto/{id} */
@RestController
@CrossOrigin
public class ProductoDirectoController {

    private final CrudDirecto<Producto> crud;

    public ProductoDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Producto.class, "idProducto", mapper);
    }

    @GetMapping("/getProductos")
    public List<Producto> getProductos() {
        return crud.listar();
    }

    @PostMapping("/insertProducto")
    public ResponseEntity<Map<String, Object>> insertProducto(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateProducto")
    public ResponseEntity<Map<String, Object>> updateProducto(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteProducto/{id}")
    public ResponseEntity<Map<String, Object>> deleteProducto(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
