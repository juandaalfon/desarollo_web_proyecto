package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.DetallePedido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de DetallePedido:  GET /getDetallesPedido  ·  POST /insertDetallePedido  ·  PUT /updateDetallePedido  ·  DELETE /deleteDetallePedido/{id} */
@RestController
@CrossOrigin
public class DetallePedidoDirectoController {

    private final CrudDirecto<DetallePedido> crud;

    public DetallePedidoDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(DetallePedido.class, "idDetalle", mapper);
    }

    @GetMapping("/getDetallesPedido")
    public List<DetallePedido> getDetallesPedido() {
        return crud.listar();
    }

    @PostMapping("/insertDetallePedido")
    public ResponseEntity<Map<String, Object>> insertDetallePedido(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateDetallePedido")
    public ResponseEntity<Map<String, Object>> updateDetallePedido(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteDetallePedido/{id}")
    public ResponseEntity<Map<String, Object>> deleteDetallePedido(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
