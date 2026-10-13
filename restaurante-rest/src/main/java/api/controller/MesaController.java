package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mesa;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
public class MesaController extends CrudController<Mesa> {

    public MesaController(ObjectMapper mapper) {
        super(Mesa.class, mapper);
    }
}
