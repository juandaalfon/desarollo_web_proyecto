package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.HistorialEstado;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de HistorialEstado:  GET /getHistorialEstados  ·  POST /insertHistorialEstado  ·  PUT /updateHistorialEstado  ·  DELETE /deleteHistorialEstado/{id} */
@RestController
@CrossOrigin
public class HistorialEstadoDirectoController {

    private final CrudDirecto<HistorialEstado> crud;

    public HistorialEstadoDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(HistorialEstado.class, "idHistorial", mapper);
    }

    @GetMapping("/getHistorialEstados")
    public List<HistorialEstado> getHistorialEstados() {
        return crud.listar();
    }

    @PostMapping("/insertHistorialEstado")
    public ResponseEntity<Map<String, Object>> insertHistorialEstado(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateHistorialEstado")
    public ResponseEntity<Map<String, Object>> updateHistorialEstado(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteHistorialEstado/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistorialEstado(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
