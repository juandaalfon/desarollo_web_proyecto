package api.service;

import api.exception.ProhibidoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.HistorialEstado;
import org.springframework.stereotype.Service;

/**
 * RF-10: el historial lo escribe el sistema al crear o cambiar de estado un pedido.
 * Por eso aquí solo se puede consultar: nadie puede crear, editar ni borrar registros a mano.
 */
@Service
public class HistorialEstadoService extends CrudService<HistorialEstado> {

    private static final String MSG = "El historial de estados se registra automáticamente y no se puede modificar.";

    public HistorialEstadoService(ObjectMapper mapper) {
        super(HistorialEstado.class, mapper);
    }

    @Override
    public HistorialEstado crear(HistorialEstado entidad) {
        throw new ProhibidoException(MSG);
    }

    @Override
    public HistorialEstado reemplazar(Integer id, HistorialEstado entidad) {
        throw new ProhibidoException(MSG);
    }

    @Override
    public HistorialEstado modificar(Integer id, JsonNode cambios) {
        throw new ProhibidoException(MSG);
    }

    @Override
    public void eliminar(Integer id) {
        throw new ProhibidoException(MSG);
    }
}
