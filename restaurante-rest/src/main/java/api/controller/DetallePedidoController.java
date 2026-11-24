package api.controller;

import api.service.DetallePedidoService;
import jakarta.servlet.http.HttpServletRequest;
import model.DetallePedido;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.Sesion;

import java.util.Map;

/**
 * RF-04 (productos del pedido) y RF-05 (observaciones del producto, campo "notas").
 *
 *   POST   /api/detalles-pedido   {"pedido": {"idPedido": 1}, "producto": {"idProducto": 2}, "cantidad": 2, "notas": "Sin cebolla"}
 *   PUT    /api/detalles-pedido/{id}   cambia cantidad y notas
 *   PATCH  /api/detalles-pedido/{id}   cambia solo lo enviado (cantidad y/o notas)
 *   DELETE /api/detalles-pedido/{id}   quita el producto del pedido
 *   GET    /api/detalles-pedido?idPedido=1   productos de un pedido
 *
 * El precio y el subtotal los calcula el servidor (no se envían).
 */
@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController extends CrudController<DetallePedido> {

    private final DetallePedidoService servicio;

    public DetallePedidoController(DetallePedidoService servicio) {
        super(servicio);
        this.servicio = servicio;
    }

    /** RF-05: registra o cambia la observación. Body: {"notas": "Sin cebolla"} (con "" se borra). */
    @PatchMapping("/{id}/notas")
    public DetallePedido registrarObservacion(@PathVariable("id") Integer id,
                                              @RequestBody Map<String, String> cuerpo,
                                              HttpServletRequest peticion) {
        Sesion sesion = Sesion.de(peticion);
        return servicio.registrarObservacion(id, cuerpo.get("notas"), sesion != null ? sesion.idUsuario() : null);
    }
}
