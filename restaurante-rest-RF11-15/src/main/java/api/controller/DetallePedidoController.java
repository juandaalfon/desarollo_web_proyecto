package api.controller;

import api.service.DetallePedidoService;
import model.DetallePedido;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController extends CrudController<DetallePedido> {

    public DetallePedidoController(DetallePedidoService servicio) {
        super(servicio);
    }
}
