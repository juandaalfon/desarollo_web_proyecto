package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Mesa;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Mesa:  GET /getMesas  ·  POST /insertMesa  ·  PUT /updateMesa  ·  DELETE /deleteMesa/{id} */
@RestController
@CrossOrigin
public class MesaDirectoController {

    private final CrudDirecto<Mesa> crud;

    public MesaDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Mesa.class, "idMesa", mapper);
    }

    @GetMapping("/getMesas")
    public List<Mesa> getMesas() {
        return crud.listar();
    }

    @PostMapping("/insertMesa")
    public ResponseEntity<Map<String, Object>> insertMesa(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateMesa")
    public ResponseEntity<Map<String, Object>> updateMesa(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteMesa/{id}")
    public ResponseEntity<Map<String, Object>> deleteMesa(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
