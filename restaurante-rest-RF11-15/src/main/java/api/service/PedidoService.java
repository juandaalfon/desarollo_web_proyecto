package api.service;

import api.dto.Dtos.EtapaDTO;
import api.dto.Dtos.HistorialPedidoDTO;
import api.dto.Dtos.ItemCocinaDTO;
import api.dto.Dtos.PedidoCocinaDTO;
import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import api.exception.RecursoNoEncontradoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import dao.PedidoDAO;
import model.DetallePedido;
import model.EstadoPedido;
import model.HistorialEstado;
import model.Mesa;
import model.Pedido;
import model.Usuario;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Reglas del pedido: creación con historial (RF-10), cola de cocina (RF-06), cambio de estado (RF-07),
 * cancelación (RF-08), entrega (RF-09) y consulta del historial con tiempos por etapa (RF-10).
 */
@Service
public class PedidoService extends CrudService<Pedido> {

    private static final String MSG_ESTADO_BLOQUEADO =
            "El estado no se cambia por aquí. Usa PATCH /api/pedidos/{id}/estado (o /cancelar, /entregar).";

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final GenericDAO<Mesa> mesaDAO = new GenericDAO<>(Mesa.class);
    private final GenericDAO<Usuario> usuarioDAO = new GenericDAO<>(Usuario.class);
    private final AccesoService acceso;

    public PedidoService(ObjectMapper mapper, AccesoService acceso) {
        super(Pedido.class, mapper);
        this.acceso = acceso;
    }

    // ---------- Creación (el pedido nace PENDIENTE y queda en el historial) ----------

    @Override
    public Pedido crear(Pedido pedido) {
        if (pedido.getMesa() == null || pedido.getMesa().getIdMesa() == null) {
            throw new OperacionInvalidaException("Debes indicar la mesa: \"mesa\": {\"idMesa\": 1}.");
        }
        if (pedido.getMesero() == null || pedido.getMesero().getIdUsuario() == null) {
            throw new OperacionInvalidaException("Debes indicar el mesero: \"mesero\": {\"idUsuario\": 1}.");
        }
        if (pedido.getNumPersonas() == null || pedido.getNumPersonas() <= 0) {
            throw new OperacionInvalidaException("El campo 'numPersonas' debe ser mayor que 0.");
        }
        Mesa mesa = mesaDAO.buscarPorId(pedido.getMesa().getIdMesa());
        if (mesa == null) {
            throw new OperacionInvalidaException("La mesa con id " + pedido.getMesa().getIdMesa() + " no existe.");
        }
        Usuario mesero = usuarioDAO.buscarPorId(pedido.getMesero().getIdUsuario());
        if (mesero == null) {
            throw new OperacionInvalidaException("El usuario con id " + pedido.getMesero().getIdUsuario() + " no existe.");
        }
        if (!Boolean.TRUE.equals(mesero.getActivo())) {
            throw new OperacionInvalidaException("El mesero '" + mesero.getNombre() + "' está inactivo.");
        }
        if (TipoRol.de(mesero.getRol().getNombreRol()) != TipoRol.MESERO) {
            throw new OperacionInvalidaException("El usuario '" + mesero.getNombre() + "' no tiene rol de mesero.");
        }

        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        pedido.setIdPedido(null);
        pedido.setMesa(mesa);
        pedido.setMesero(mesero);
        pedido.setEstado(EstadoPedido.PENDIENTE.name());
        pedido.setFechaCreacion(ahora);
        pedido.setFechaUltimaActualizacion(ahora);
        try {
            return buscar(pedidoDAO.crearConHistorial(pedido).getIdPedido());
        } catch (RuntimeException e) {
            if (e instanceof OperacionInvalidaException || e instanceof RecursoNoEncontradoException) {
                throw e;
            }
            throw new OperacionInvalidaException("No se pudo crear el pedido.");
        }
    }

    // ---------- El estado solo cambia por los endpoints del flujo ----------

    @Override
    public Pedido reemplazar(Integer id, Pedido entidad) {
        Pedido actual = buscar(id);
        if (entidad.getEstado() != null && !mismoEstado(entidad.getEstado(), actual.getEstado())) {
            throw new OperacionInvalidaException(MSG_ESTADO_BLOQUEADO);
        }
        entidad.setEstado(actual.getEstado());
        if (entidad.getFechaCreacion() == null) {
            entidad.setFechaCreacion(actual.getFechaCreacion());
        }
        return super.reemplazar(id, entidad);
    }

    @Override
    public Pedido modificar(Integer id, JsonNode cambios) {
        if (cambios != null && cambios.has("estado")) {
            throw new OperacionInvalidaException(MSG_ESTADO_BLOQUEADO);
        }
        return super.modificar(id, cambios);
    }

    private boolean mismoEstado(String a, String b) {
        try {
            return EstadoPedido.desdeTexto(a) == EstadoPedido.desdeTexto(b);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ---------- RF-06: cola de pedidos pendientes para cocina ----------

    public List<PedidoCocinaDTO> colaCocina(Integer idUsuario) {
        acceso.exigirRol(idUsuario, TipoRol.COCINA, TipoRol.ADMINISTRADOR);
        List<Pedido> pendientes = pedidoDAO.listarPorEstado(EstadoPedido.PENDIENTE);

        Map<Integer, List<ItemCocinaDTO>> itemsPorPedido = new HashMap<>();
        List<Integer> ids = pendientes.stream().map(Pedido::getIdPedido).collect(Collectors.toList());
        for (DetallePedido d : pedidoDAO.detallesDePedidos(ids)) {
            itemsPorPedido
                    .computeIfAbsent(d.getPedido().getIdPedido(), k -> new ArrayList<>())
                    .add(new ItemCocinaDTO(d.getIdDetalle(), d.getProducto().getNombre(), d.getCantidad(), d.getNotas()));
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<PedidoCocinaDTO> cola = new ArrayList<>();
        for (Pedido p : pendientes) {
            long minutos = Math.max(0, Duration.between(p.getFechaCreacion(), ahora).toMinutes());
            cola.add(new PedidoCocinaDTO(p.getIdPedido(), p.getMesa().getNumero(), p.getMesero().getNombre(),
                    p.getNumPersonas(), p.getEstado(), p.getFechaCreacion(), minutos,
                    itemsPorPedido.getOrDefault(p.getIdPedido(), new ArrayList<>())));
        }
        return cola;
    }

    // ---------- RF-07 / RF-08 / RF-09: cambios de estado ----------

    /** RF-07: cambia el estado respetando el flujo y el rol de quien lo pide. Registra el historial (RF-10). */
    public Pedido cambiarEstado(Integer id, String estadoTexto, Integer idUsuario) {
        EstadoPedido destino;
        try {
            destino = EstadoPedido.desdeTexto(estadoTexto);
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException(e.getMessage());
        }
        Usuario usuario = acceso.exigirRol(idUsuario, rolesPara(destino));

        Pedido actual = buscar(id);
        EstadoPedido origen;
        try {
            origen = EstadoPedido.desdeTexto(actual.getEstado());
        } catch (IllegalArgumentException e) {
            throw new ConflictoException("El pedido tiene un estado no reconocido: '" + actual.getEstado() + "'.");
        }
        if (!origen.puedePasarA(destino)) {
            throw new ConflictoException(origen.mensajeTransicion(destino));
        }

        try {
            pedidoDAO.cambiarEstado(id, destino, usuario);
        } catch (IllegalStateException e) {
            throw new ConflictoException(e.getMessage());
        } catch (NoSuchElementException e) {
            throw new RecursoNoEncontradoException(e.getMessage());
        } catch (RuntimeException e) {
            throw new OperacionInvalidaException("No se pudo cambiar el estado del pedido.");
        }
        return buscar(id);
    }

    /** RF-08: cancelar (solo desde PENDIENTE o EN_PREPARACION). */
    public Pedido cancelar(Integer id, Integer idUsuario) {
        return cambiarEstado(id, EstadoPedido.CANCELADO.name(), idUsuario);
    }

    /** RF-09: el mesero marca el pedido como entregado (debe estar LISTO). */
    public Pedido entregar(Integer id, Integer idUsuario) {
        return cambiarEstado(id, EstadoPedido.ENTREGADO.name(), idUsuario);
    }

    /** Quién puede llevar un pedido a cada estado. El administrador puede todo. */
    private TipoRol[] rolesPara(EstadoPedido destino) {
        switch (destino) {
            case EN_PREPARACION:
            case LISTO:
                return new TipoRol[]{TipoRol.COCINA, TipoRol.ADMINISTRADOR};
            case ENTREGADO:
            case CANCELADO:
                return new TipoRol[]{TipoRol.MESERO, TipoRol.ADMINISTRADOR};
            default:
                return new TipoRol[]{TipoRol.ADMINISTRADOR};
        }
    }

    // ---------- Consultas del pedido ----------

    public List<DetallePedido> detalles(Integer id) {
        buscar(id); // 404 si no existe
        return pedidoDAO.detallesDePedido(id);
    }

    /** RF-10: cada estado con su fecha/hora, quién lo cambió y cuánto duró. */
    public HistorialPedidoDTO historial(Integer id) {
        Pedido pedido = buscar(id);
        List<HistorialEstado> filas = pedidoDAO.historialDePedido(id);

        List<EtapaDTO> etapas = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            HistorialEstado h = filas.get(i);
            Long duracion = null;
            if (i + 1 < filas.size()) {
                duracion = Duration.between(h.getFechaHora(), filas.get(i + 1).getFechaHora()).getSeconds();
            }
            etapas.add(new EtapaDTO(h.getEstado(), h.getFechaHora(), h.getUsuario().getIdUsuario(),
                    h.getUsuario().getNombre(), duracion));
        }

        boolean finalizado = false;
        try {
            finalizado = EstadoPedido.desdeTexto(pedido.getEstado()).esFinal();
        } catch (IllegalArgumentException ignorado) {
            // estado no reconocido: se trata como no finalizado
        }
        long total = 0;
        if (!filas.isEmpty()) {
            LocalDateTime inicio = filas.get(0).getFechaHora();
            LocalDateTime fin = finalizado ? filas.get(filas.size() - 1).getFechaHora() : LocalDateTime.now();
            total = Math.max(0, Duration.between(inicio, fin).getSeconds());
        }
        return new HistorialPedidoDTO(id, pedido.getEstado(), finalizado, total, etapas);
    }
}
