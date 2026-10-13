package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Pedido;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController extends CrudController<Pedido> {

    public PedidoController(ObjectMapper mapper) {
        super(Pedido.class, mapper);
    }
}
