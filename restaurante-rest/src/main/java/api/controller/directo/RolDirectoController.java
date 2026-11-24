package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Rol;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Rol:  GET /getRoles  ·  POST /insertRol  ·  PUT /updateRol  ·  DELETE /deleteRol/{id} */
@RestController
@CrossOrigin
public class RolDirectoController {

    private final CrudDirecto<Rol> crud;

    public RolDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Rol.class, "idRol", mapper);
    }

    @GetMapping("/getRoles")
    public List<Rol> getRoles() {
        return crud.listar();
    }

    @PostMapping("/insertRol")
    public ResponseEntity<Map<String, Object>> insertRol(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateRol")
    public ResponseEntity<Map<String, Object>> updateRol(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteRol/{id}")
    public ResponseEntity<Map<String, Object>> deleteRol(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
