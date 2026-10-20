package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import model.DetallePedido;
import org.springframework.web.bind.annotation.*;
import security.Sesion;
import service.DetallePedidoService;

import java.util.List;
import java.util.Map;

/**
 * RF-04 (productos del pedido) y RF-05 (observaciones del producto, campo "notas").
 *
 *   POST   /api/detalles-pedido   {"pedido": {"idPedido": 1}, "producto": {"idProducto": 2}, "cantidad": 2, "notas": "Sin cebolla"}
 *   PUT    /api/detalles-pedido/{id}   cambia cantidad y notas
 *   DELETE /api/detalles-pedido/{id}   quita el producto del pedido
 *   GET    /api/detalles-pedido?idPedido=1   productos de un pedido
 *
 * El precio y el subtotal los calcula el servidor (no se envían).
 *
 * Endpoint propio de RF-05:
 *   PATCH  /api/detalles-pedido/{id}/notas   {"notas": "Sin cebolla y sin picante"}   (con "" borra la observación)
 */
@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController extends CrudController<DetallePedido> {

    private final DetallePedidoService servicio = new DetallePedidoService();

    public DetallePedidoController(ObjectMapper mapper) {
        super(DetallePedido.class, mapper);
    }

    @Override
    protected List<DetallePedido> listarEntidades(Map<String, String> filtros) {
        String idPedido = filtros.get("idPedido");
        return servicio.listar(idPedido == null || idPedido.isBlank() ? null : Integer.valueOf(idPedido.trim()));
    }

    @Override
    protected DetallePedido procesarCreacion(DetallePedido d, Sesion sesion) {
        Integer idPedido = d.getPedido() != null ? d.getPedido().getIdPedido() : null;
        Integer idProducto = d.getProducto() != null ? d.getProducto().getIdProducto() : null;
        return servicio.agregar(idPedido, idProducto, d.getCantidad(), d.getNotas(), sesion);
    }

    @Override
    protected DetallePedido procesarActualizacion(DetallePedido d, Sesion sesion) {
        // Solo cantidad y notas son editables; el pedido, el producto y el precio no se pueden cambiar
        return servicio.actualizar(d.getIdDetalle(), d.getCantidad(), true, d.getNotas(), sesion);
    }

    @Override
    protected void procesarEliminacion(Integer id, Sesion sesion) {
        servicio.eliminar(id, sesion);
    }

    // ------------------------------------------------------------ RF-05

    @PatchMapping("/{id}/notas")
    public DetallePedido registrarObservacion(@PathVariable Integer id, @RequestBody Map<String, String> cuerpo,
                                              HttpServletRequest peticion) {
        return servicio.registrarObservacion(id, cuerpo.get("notas"), sesion(peticion));
    }
}
