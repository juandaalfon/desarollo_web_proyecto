package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.DetallePedido;
import org.springframework.stereotype.Service;

@Service
public class DetallePedidoService extends CrudService<DetallePedido> {

    public DetallePedidoService(ObjectMapper mapper) {
        super(DetallePedido.class, mapper);
    }
}
