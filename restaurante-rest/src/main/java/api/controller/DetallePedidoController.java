package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.DetallePedido;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController extends CrudController<DetallePedido> {

    public DetallePedidoController(ObjectMapper mapper) {
        super(DetallePedido.class, mapper);
    }
}
