package api.controller;

import api.service.FacturaService;
import model.Factura;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController extends CrudController<Factura> {

    public FacturaController(FacturaService servicio) {
        super(servicio);
    }
}
