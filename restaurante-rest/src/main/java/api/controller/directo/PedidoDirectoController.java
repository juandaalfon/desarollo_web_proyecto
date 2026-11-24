package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Pedido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Pedido:  GET /getPedidos  ·  POST /insertPedido  ·  PUT /updatePedido  ·  DELETE /deletePedido/{id} */
@RestController
@CrossOrigin
public class PedidoDirectoController {

    private final CrudDirecto<Pedido> crud;

    public PedidoDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Pedido.class, "idPedido", mapper);
    }

    @GetMapping("/getPedidos")
    public List<Pedido> getPedidos() {
        return crud.listar();
    }

    @PostMapping("/insertPedido")
    public ResponseEntity<Map<String, Object>> insertPedido(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updatePedido")
    public ResponseEntity<Map<String, Object>> updatePedido(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deletePedido/{id}")
    public ResponseEntity<Map<String, Object>> deletePedido(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
