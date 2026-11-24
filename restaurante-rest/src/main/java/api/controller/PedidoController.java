package api.controller;

import api.dto.Dtos.CambioEstadoRequest;
import api.dto.Dtos.HistorialPedidoDTO;
import api.service.PedidoService;
import jakarta.servlet.http.HttpServletRequest;
import model.DetallePedido;
import model.Pedido;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import security.Sesion;

import java.util.List;
import java.util.Map;

/**
 * CRUD de pedidos (heredado) más el flujo del pedido.
 * Las acciones del flujo las realiza el usuario de la sesión (header Authorization: Bearer <token>).
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController extends CrudController<Pedido> {

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        super(servicio);
        this.servicio = servicio;
    }

    /** RF-03: asigna el pedido a otra mesa (debe estar LIBRE y alcanzar para las personas). Body: {"idMesa": 3}. */
    @PutMapping("/{id}/mesa")
    public Pedido asignarMesa(@PathVariable("id") Integer id,
                              @RequestBody Map<String, Integer> cuerpo,
                              HttpServletRequest peticion) {
        return servicio.asignarMesa(id, cuerpo.get("idMesa"), idUsuario(peticion));
    }

    /** RF-07: cambia el estado. Body: {"estado": "EN_PREPARACION"}. */
    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable("id") Integer id,
                                @RequestBody CambioEstadoRequest cuerpo,
                                HttpServletRequest peticion) {
        return servicio.cambiarEstado(id, cuerpo.estado(), idUsuario(peticion));
    }

    /** RF-08: cancela el pedido (solo PENDIENTE o EN_PREPARACION). Sin body. */
    @PatchMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable("id") Integer id,
                           HttpServletRequest peticion) {
        return servicio.cancelar(id, idUsuario(peticion));
    }

    /** RF-09: el mesero marca el pedido como entregado (debe estar LISTO). Sin body. */
    @PatchMapping("/{id}/entregar")
    public Pedido entregar(@PathVariable("id") Integer id,
                           HttpServletRequest peticion) {
        return servicio.entregar(id, idUsuario(peticion));
    }

    /** RF-10: historial del pedido con fecha, hora y duración de cada etapa. */
    @GetMapping("/{id}/historial")
    public HistorialPedidoDTO historial(@PathVariable("id") Integer id) {
        return servicio.historial(id);
    }

    /** Productos del pedido. */
    @GetMapping("/{id}/detalles")
    public List<DetallePedido> detalles(@PathVariable("id") Integer id) {
        return servicio.detalles(id);
    }

    private static Integer idUsuario(HttpServletRequest peticion) {
        Sesion sesion = Sesion.de(peticion);
        return sesion != null ? sesion.idUsuario() : null;
    }
}
