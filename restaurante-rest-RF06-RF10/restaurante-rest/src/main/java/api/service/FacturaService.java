package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Factura;
import org.springframework.stereotype.Service;

@Service
public class FacturaService extends CrudService<Factura> {

    public FacturaService(ObjectMapper mapper) {
        super(Factura.class, mapper);
    }
}
