package api.controller.directo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CRUD directo de Usuario:  GET /getUsuarios  ·  POST /insertUsuario  ·  PUT /updateUsuario  ·  DELETE /deleteUsuario/{id} */
@RestController
@CrossOrigin
public class UsuarioDirectoController {

    private final CrudDirecto<Usuario> crud;

    public UsuarioDirectoController(ObjectMapper mapper) {
        this.crud = new CrudDirecto<>(Usuario.class, "idUsuario", mapper);
    }

    @GetMapping("/getUsuarios")
    public List<Usuario> getUsuarios() {
        return crud.listar();
    }

    @PostMapping("/insertUsuario")
    public ResponseEntity<Map<String, Object>> insertUsuario(@RequestBody JsonNode cuerpo) {
        return crud.insertar(cuerpo);
    }

    @PutMapping("/updateUsuario")
    public ResponseEntity<Map<String, Object>> updateUsuario(@RequestBody JsonNode cuerpo) {
        return crud.actualizar(cuerpo);
    }

    @DeleteMapping("/deleteUsuario/{id}")
    public ResponseEntity<Map<String, Object>> deleteUsuario(@PathVariable("id") Integer id) {
        return crud.eliminar(id);
    }
}
