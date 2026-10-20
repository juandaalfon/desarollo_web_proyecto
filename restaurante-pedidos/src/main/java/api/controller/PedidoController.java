package api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import model.Pedido;
import org.springframework.web.bind.annotation.*;
import security.Sesion;
import service.PedidoService;
import service.ServicioException;

import java.util.List;
import java.util.Map;

/**
 * RF-02 (gestión de pedidos) y RF-03 (asignación de mesa).
 *
 * El CRUD genérico pasa por PedidoService para que se cumplan las reglas:
 *   POST   /api/pedidos                {"mesa": {"idMesa": 1}, "numPersonas": 2}   crea el pedido (mesero = quien inició sesión)
 *   PUT    /api/pedidos/{id}           cambia mesa y/o número de personas
 *   DELETE /api/pedidos/{id}           solo pedidos PENDIENTE o CANCELADO
 *   GET    /api/pedidos?estado=LISTO   lista filtrando por estado
 *
 * Endpoints propios:
 *   PUT    /api/pedidos/{id}/mesa      {"idMesa": 3}          RF-03: asigna el pedido a otra mesa
 *   PATCH  /api/pedidos/{id}/estado    {"estado": "LISTO"}    avanza el pedido (cocina y mesero); queda en el historial
 *   GET    /api/pedidos/{id}/detalles  pedido + productos + observaciones + total
 *
 * Nota: el estado no se cambia con PUT/PATCH /api/pedidos/{id}; para eso existe /estado.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController extends CrudController<Pedido> {

    private final PedidoService servicio = new PedidoService();

    public PedidoController(ObjectMapper mapper) {
        super(Pedido.class, mapper);
    }

    @Override
    protected List<Pedido> listarEntidades(Map<String, String> filtros) {
        return servicio.listar(filtros.get("estado"));
    }

    @Override
    protected Pedido procesarCreacion(Pedido pedido, Sesion sesion) {
        return servicio.crear(idMesa(pedido), pedido.getNumPersonas(), sesion);
    }

    @Override
    protected Pedido procesarActualizacion(Pedido pedido, Sesion sesion) {
        return servicio.actualizar(pedido.getIdPedido(), idMesa(pedido), pedido.getNumPersonas(), sesion);
    }

    @Override
    protected void procesarEliminacion(Integer id, Sesion sesion) {
        servicio.eliminar(id, sesion);
    }

    // ------------------------------------------------------------ RF-03

    @PutMapping("/{id}/mesa")
    public Pedido asignarMesa(@PathVariable Integer id, @RequestBody Map<String, Integer> cuerpo,
                              HttpServletRequest peticion) {
        Integer idMesa = cuerpo.get("idMesa");
        if (idMesa == null) {
            throw ServicioException.invalida("Falta el campo idMesa. Ejemplo: {\"idMesa\": 3}");
        }
        return servicio.asignarMesa(id, idMesa, sesion(peticion));
    }

    // ------------------------------------------------------------ RF-02

    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, String> cuerpo,
                                HttpServletRequest peticion) {
        return servicio.cambiarEstado(id, cuerpo.get("estado"), sesion(peticion));
    }

    @GetMapping("/{id}/detalles")
    public Map<String, Object> resumen(@PathVariable Integer id) {
        return servicio.resumen(id);
    }

    private static Integer idMesa(Pedido pedido) {
        return pedido.getMesa() != null ? pedido.getMesa().getIdMesa() : null;
    }
}
