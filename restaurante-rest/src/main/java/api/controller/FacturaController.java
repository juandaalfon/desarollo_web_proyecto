package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Factura;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController extends CrudController<Factura> {

    public FacturaController(ObjectMapper mapper) {
        super(Factura.class, mapper);
    }
}
