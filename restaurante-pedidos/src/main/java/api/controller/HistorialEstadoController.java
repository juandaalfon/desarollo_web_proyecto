package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.HistorialEstado;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RF-02: historial de cambios de estado de los pedidos. Lo llena automáticamente el servicio de pedidos;
 * todos los roles lo consultan (GET /api/historial-estados?idPedido=5) y solo Administración lo edita a mano.
 */
@RestController
@RequestMapping("/api/historial-estados")
public class HistorialEstadoController extends CrudController<HistorialEstado> {

    public HistorialEstadoController(ObjectMapper mapper) {
        super(HistorialEstado.class, mapper);
    }

    @Override
    protected List<HistorialEstado> listarEntidades(Map<String, String> filtros) {
        List<HistorialEstado> todos = super.listarEntidades(filtros);
        String idPedido = filtros.get("idPedido");
        if (idPedido == null || idPedido.isBlank()) {
            return todos;
        }
        return todos.stream()
                .filter(h -> h.getPedido() != null && idPedido.trim().equals(String.valueOf(h.getPedido().getIdPedido())))
                .toList();
    }
}
