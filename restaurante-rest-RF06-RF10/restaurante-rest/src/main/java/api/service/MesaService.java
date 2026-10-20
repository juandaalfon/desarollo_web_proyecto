package api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mesa;
import org.springframework.stereotype.Service;

@Service
public class MesaService extends CrudService<Mesa> {

    public MesaService(ObjectMapper mapper) {
        super(Mesa.class, mapper);
    }
}
