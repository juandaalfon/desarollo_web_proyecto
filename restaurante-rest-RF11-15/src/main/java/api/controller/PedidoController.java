package api.controller;

import api.dto.Dtos.CambioEstadoRequest;
import api.dto.Dtos.HistorialPedidoDTO;
import api.service.PedidoService;
import model.DetallePedido;
import model.Pedido;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD de pedidos (heredado) más el flujo del pedido.
 * Las acciones del flujo llevan el encabezado X-Usuario-Id con el usuario que las realiza.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController extends CrudController<Pedido> {

    private static final String USUARIO = "X-Usuario-Id";

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        super(servicio);
        this.servicio = servicio;
    }

    /** RF-07: cambia el estado. Body: {"estado": "EN_PREPARACION"}. */
    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable("id") Integer id,
                                @RequestBody CambioEstadoRequest cuerpo,
                                @RequestHeader(value = USUARIO, required = false) Integer idUsuario) {
        return servicio.cambiarEstado(id, cuerpo.estado(), idUsuario);
    }

    /** RF-08: cancela el pedido (solo PENDIENTE o EN_PREPARACION). Sin body. */
    @PatchMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable("id") Integer id,
                           @RequestHeader(value = USUARIO, required = false) Integer idUsuario) {
        return servicio.cancelar(id, idUsuario);
    }

    /** RF-09: el mesero marca el pedido como entregado (debe estar LISTO). Sin body. */
    @PatchMapping("/{id}/entregar")
    public Pedido entregar(@PathVariable("id") Integer id,
                           @RequestHeader(value = USUARIO, required = false) Integer idUsuario) {
        return servicio.entregar(id, idUsuario);
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
}
