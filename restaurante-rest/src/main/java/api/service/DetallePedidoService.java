package api.service;

import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import api.exception.ProhibidoException;
import api.exception.RecursoNoEncontradoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.DetallePedidoDAO;
import dao.GenericDAO;
import model.DetallePedido;
import model.Pedido;
import model.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * RF-04 (productos del pedido) y RF-05 (observaciones del producto, columna "notas").
 *
 * Reglas:
 *  - Solo se agregan productos disponibles; el precio se copia del producto en ese momento
 *    y el subtotal se calcula como precio x cantidad (no se envían).
 *  - Los productos de un pedido solo se modifican mientras esté PENDIENTE o EN_PREPARACION.
 *  - Un mesero solo modifica los pedidos propios; el administrador todos; la cocina solo consulta.
 *
 * El usuario que actúa sale de la sesión: los métodos del CRUD genérico lo toman de la petición en curso y los
 * demás lo reciben como parámetro (los usa la API y también la consola).
 */
@Service
public class DetallePedidoService extends CrudService<DetallePedido> {

    private static final int MAX_NOTAS = 255;

    private final DetallePedidoDAO detalleDAO = new DetallePedidoDAO();
    private final GenericDAO<Pedido> pedidoDAO = new GenericDAO<>(Pedido.class);
    private final AccesoService acceso;

    public DetallePedidoService(ObjectMapper mapper, AccesoService acceso) {
        super(DetallePedido.class, mapper);
        this.acceso = acceso;
    }

    // ---------- consultas ----------

    /** GET /api/detalles-pedido?idPedido=1  (sin idPedido devuelve todos). */
    @Override
    public List<DetallePedido> listar(Map<String, String> filtros) {
        String idPedido = filtros.get("idPedido");
        if (idPedido == null || idPedido.isBlank()) {
            return listar();
        }
        try {
            return listarPorPedido(Integer.valueOf(idPedido.trim()));
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El valor '" + idPedido + "' no es válido para 'idPedido'.");
        }
    }

    /** Productos de un pedido, en el orden en que se agregaron. */
    public List<DetallePedido> listarPorPedido(Integer idPedido) {
        return dao.listar().stream()
                .filter(d -> d.getPedido() != null && idPedido.equals(d.getPedido().getIdPedido()))
                .sorted((a, b) -> a.getIdDetalle().compareTo(b.getIdDetalle()))
                .collect(Collectors.toList());
    }

    // ---------- RF-04: agregar, cambiar y quitar productos ----------

    @Override
    public DetallePedido crear(DetallePedido d) {
        Integer idPedido = d.getPedido() != null ? d.getPedido().getIdPedido() : null;
        Integer idProducto = d.getProducto() != null ? d.getProducto().getIdProducto() : null;
        return agregar(idPedido, idProducto, d.getCantidad(), d.getNotas(), acceso.usuarioActualId());
    }

    /** Agrega un producto al pedido. "notas" (observación para cocina) es opcional: RF-05. */
    public DetallePedido agregar(Integer idPedido, Integer idProducto, Integer cantidad, String notas,
                                 Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        if (idPedido == null || idProducto == null) {
            throw new OperacionInvalidaException("El pedido y el producto son obligatorios: "
                    + "\"pedido\": {\"idPedido\": 1}, \"producto\": {\"idProducto\": 2}.");
        }
        validarCantidad(cantidad);
        String observacion = limpiarNotas(notas);

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            throw new RecursoNoEncontradoException("El pedido " + idPedido + " no existe.");
        }
        verificarPropietario(pedido, actor);
        try {
            DetallePedido creado = detalleDAO.agregar(idPedido, idProducto, cantidad, observacion);
            return buscar(creado.getIdDetalle());
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    /** PUT: solo cantidad y notas son editables; el pedido, el producto y el precio no se pueden cambiar. */
    @Override
    public DetallePedido reemplazar(Integer id, DetallePedido d) {
        return actualizar(id, d.getCantidad(), true, d.getNotas(), acceso.usuarioActualId());
    }

    /** PATCH: cambia solo lo que venga en el JSON ("cantidad" y/o "notas"). */
    @Override
    public DetallePedido modificar(Integer id, JsonNode cambios) {
        Integer cantidad = null;
        if (cambios != null && cambios.hasNonNull("cantidad")) {
            if (!cambios.get("cantidad").isInt()) {
                throw new OperacionInvalidaException("La cantidad debe ser un número entero.");
            }
            cantidad = cambios.get("cantidad").asInt();
        }
        boolean tocarNotas = cambios != null && cambios.has("notas");
        String notas = tocarNotas && !cambios.get("notas").isNull() ? cambios.get("notas").asText() : null;
        return actualizar(id, cantidad, tocarNotas, notas, acceso.usuarioActualId());
    }

    /**
     * @param cantidad   nueva cantidad, o null para no cambiarla
     * @param tocarNotas true si se debe reemplazar la observación (con notas null o vacío se borra)
     */
    public DetallePedido actualizar(Integer idDetalle, Integer cantidad, boolean tocarNotas, String notas,
                                    Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        if (cantidad != null) {
            validarCantidad(cantidad);
        }
        String observacion = tocarNotas ? limpiarNotas(notas) : null;

        verificarPropietario(buscar(idDetalle).getPedido(), actor);
        try {
            detalleDAO.actualizar(idDetalle, cantidad, tocarNotas, observacion);
            return buscar(idDetalle);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    @Override
    public void eliminar(Integer id) {
        eliminar(id, acceso.usuarioActualId());
    }

    /** Quita el producto del pedido. */
    public void eliminar(Integer idDetalle, Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        verificarPropietario(buscar(idDetalle).getPedido(), actor);
        try {
            detalleDAO.eliminar(idDetalle);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    // ---------- RF-05: observaciones ----------

    /** Registra, cambia o borra (con texto vacío) la observación de un producto del pedido. */
    public DetallePedido registrarObservacion(Integer idDetalle, String notas, Integer idUsuario) {
        return actualizar(idDetalle, null, true, notas, idUsuario);
    }

    // ---------- ayudas ----------

    private void verificarPropietario(Pedido pedido, Usuario actor) {
        if (TipoRol.de(actor.getRol().getNombreRol()) == TipoRol.MESERO
                && !pedido.getMesero().getIdUsuario().equals(actor.getIdUsuario())) {
            throw new ProhibidoException("Este pedido pertenece a otro mesero.");
        }
    }

    private void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw new OperacionInvalidaException("La cantidad debe ser al menos 1.");
        }
    }

    private String limpiarNotas(String notas) {
        if (notas == null || notas.isBlank()) {
            return null;
        }
        String limpia = notas.trim();
        if (limpia.length() > MAX_NOTAS) {
            throw new OperacionInvalidaException("La observación puede tener máximo " + MAX_NOTAS + " caracteres.");
        }
        return limpia;
    }

    /** Convierte los errores del DAO en las excepciones de la API (404, 409). */
    private RuntimeException traducir(RuntimeException e) {
        if (e instanceof NoSuchElementException) {
            return new RecursoNoEncontradoException(e.getMessage());
        }
        if (e instanceof IllegalStateException) {
            return new ConflictoException(e.getMessage());
        }
        return e;
    }
}
