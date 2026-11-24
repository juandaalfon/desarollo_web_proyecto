package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Factura;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Factura:  GET /getFacturas  ·  POST /insertFactura  ·  PUT /updateFactura  ·  DELETE /deleteFactura/{id} */
@RestController
@CrossOrigin
public class FacturaDirectoController {

    private final CrudDirecto<Factura> crud;

    public FacturaDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Factura.class, "idFactura", mapper);
    }

    @GetMapping("/getFacturas")
    public List<Factura> getFacturas() {
        return crud.listar();
    }

    @PostMapping("/insertFactura")
    public ResponseEntity<Map<String, Object>> insertFactura(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateFactura")
    public ResponseEntity<Map<String, Object>> updateFactura(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteFactura/{id}")
    public ResponseEntity<Map<String, Object>> deleteFactura(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
