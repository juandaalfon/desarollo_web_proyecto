package api.service;

import api.dto.Dtos.EtapaDTO;
import api.dto.Dtos.HistorialPedidoDTO;
import api.dto.Dtos.ItemCocinaDTO;
import api.dto.Dtos.PedidoCocinaDTO;
import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import api.exception.ProhibidoException;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Reglas del pedido: creación por el mesero (RF-02), asignación de mesa (RF-03), historial (RF-10),
 * cola de cocina (RF-06), cambio de estado (RF-07), cancelación (RF-08), entrega (RF-09) y consulta del
 * historial con tiempos por etapa (RF-10).
 *
 * El usuario que actúa sale de la sesión (token). Los métodos del CRUD genérico lo toman de la petición en curso
 * (AccesoService.usuarioActualId) y los demás lo reciben como parámetro (los usa la API y también la consola).
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

    // ---------- RF-02: creación (el pedido nace PENDIENTE, a nombre del mesero, y queda en el historial) ----------

    /** POST /api/pedidos: crea el pedido a nombre del usuario con sesión iniciada. */
    @Override
    public Pedido crear(Pedido pedido) {
        return crear(pedido, acceso.usuarioActualId());
    }

    /** Atajo para la consola: pedido para una mesa y un número de personas. */
    public Pedido crear(Integer idMesa, Integer numPersonas, Integer idUsuario) {
        Pedido pedido = new Pedido();
        Mesa mesa = new Mesa();
        mesa.setIdMesa(idMesa);
        pedido.setMesa(mesa);
        pedido.setNumPersonas(numPersonas);
        return crear(pedido, idUsuario);
    }

    /**
     * El mesero es quien tiene la sesión. Si quien crea es un administrador, debe indicar el mesero en el body
     * ("mesero": {"idUsuario": 1}).
     */
    public Pedido crear(Pedido pedido, Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        if (pedido.getMesa() == null || pedido.getMesa().getIdMesa() == null) {
            throw new OperacionInvalidaException("Debes indicar la mesa: \"mesa\": {\"idMesa\": 1}.");
        }
        if (pedido.getNumPersonas() == null || pedido.getNumPersonas() <= 0) {
            throw new OperacionInvalidaException("El campo 'numPersonas' debe ser mayor que 0.");
        }
        Mesa mesa = mesaDAO.buscarPorId(pedido.getMesa().getIdMesa());
        if (mesa == null) {
            throw new OperacionInvalidaException("La mesa con id " + pedido.getMesa().getIdMesa() + " no existe.");
        }

        Usuario mesero = actor;
        if (TipoRol.de(actor.getRol().getNombreRol()) == TipoRol.ADMINISTRADOR) {
            if (pedido.getMesero() == null || pedido.getMesero().getIdUsuario() == null) {
                throw new OperacionInvalidaException(
                        "Como administrador debes indicar el mesero: \"mesero\": {\"idUsuario\": 1}.");
            }
            mesero = usuarioDAO.buscarPorId(pedido.getMesero().getIdUsuario());
            if (mesero == null) {
                throw new OperacionInvalidaException("El usuario con id " + pedido.getMesero().getIdUsuario() + " no existe.");
            }
            if (!Boolean.TRUE.equals(mesero.getActivo())) {
                throw new OperacionInvalidaException("El mesero '" + mesero.getNombre() + "' está inactivo.");
            }
            if (TipoRol.de(mesero.getRol().getNombreRol()) != TipoRol.MESERO) {
                throw new OperacionInvalidaException("El usuario '" + mesero.getNombre() + "' no tiene rol de mesero.");
            }
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
            throw traducir(e, "No se pudo crear el pedido.");
        }
    }

    // ---------- RF-03: mesa y número de personas ----------

    /** PUT /api/pedidos/{id}/mesa: asigna el pedido a otra mesa. */
    public Pedido asignarMesa(Integer id, Integer idMesa, Integer idUsuario) {
        if (idMesa == null) {
            throw new OperacionInvalidaException("Falta el campo idMesa. Ejemplo: {\"idMesa\": 3}");
        }
        return cambiarMesaYPersonas(id, idMesa, null, idUsuario);
    }

    /** Cambia la mesa y/o el número de personas (null = no se modifica). Solo en pedidos que no han terminado. */
    public Pedido cambiarMesaYPersonas(Integer id, Integer idMesa, Integer numPersonas, Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        if (numPersonas != null && numPersonas <= 0) {
            throw new OperacionInvalidaException("El campo 'numPersonas' debe ser mayor que 0.");
        }
        verificarPropietario(buscar(id), actor);
        try {
            pedidoDAO.cambiarMesaYPersonas(id, idMesa, numPersonas);
        } catch (RuntimeException e) {
            throw traducir(e, "No se pudo actualizar el pedido.");
        }
        return buscar(id);
    }

    /** Solo se eliminan pedidos PENDIENTE o CANCELADO (se borran también sus productos y su historial). */
    @Override
    public void eliminar(Integer id) {
        eliminar(id, acceso.usuarioActualId());
    }

    public void eliminar(Integer id, Integer idUsuario) {
        Usuario actor = acceso.exigirRol(idUsuario, TipoRol.MESERO, TipoRol.ADMINISTRADOR);
        verificarPropietario(buscar(id), actor);
        try {
            pedidoDAO.eliminarConDependencias(id);
        } catch (RuntimeException e) {
            throw traducir(e, "No se puede eliminar: otro registro depende de este (llave foránea).");
        }
    }

    // ---------- Listado con filtro ----------

    /** GET /api/pedidos?estado=PENDIENTE (del más reciente al más antiguo). */
    @Override
    public List<Pedido> listar(Map<String, String> filtros) {
        String estado = filtros.get("estado");
        List<Pedido> pedidos = new ArrayList<>(dao.listar());
        if (estado != null && !estado.isBlank()) {
            EstadoPedido buscado;
            try {
                buscado = EstadoPedido.desdeTexto(estado);
            } catch (IllegalArgumentException e) {
                throw new OperacionInvalidaException(e.getMessage());
            }
            pedidos = pedidos.stream()
                    .filter(p -> buscado.name().equals(p.getEstado() == null ? null : p.getEstado().trim().toUpperCase()))
                    .collect(Collectors.toList());
        }
        pedidos.sort(Comparator.comparing(Pedido::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())));
        return pedidos;
    }

    // ---------- El estado solo cambia por los endpoints del flujo ----------

    /** PUT: el estado no se toca aquí; solo cambian la mesa y el número de personas (RF-03). */
    @Override
    public Pedido reemplazar(Integer id, Pedido entidad) {
        Pedido actual = buscar(id);
        if (entidad.getEstado() != null && !mismoEstado(entidad.getEstado(), actual.getEstado())) {
            throw new OperacionInvalidaException(MSG_ESTADO_BLOQUEADO);
        }
        Integer idMesa = entidad.getMesa() != null ? entidad.getMesa().getIdMesa() : null;
        return cambiarMesaYPersonas(id, idMesa, entidad.getNumPersonas(), acceso.usuarioActualId());
    }

    /** PATCH: igual que PUT; solo se leen "mesa" y "numPersonas" del JSON. */
    @Override
    public Pedido modificar(Integer id, JsonNode cambios) {
        if (cambios != null && cambios.has("estado")) {
            throw new OperacionInvalidaException(MSG_ESTADO_BLOQUEADO);
        }
        Integer idMesa = null;
        Integer numPersonas = null;
        if (cambios != null) {
            JsonNode mesa = cambios.path("mesa").path("idMesa");
            if (mesa.isInt()) {
                idMesa = mesa.asInt();
            }
            JsonNode personas = cambios.path("numPersonas");
            if (personas.isInt()) {
                numPersonas = personas.asInt();
            }
        }
        return cambiarMesaYPersonas(id, idMesa, numPersonas, acceso.usuarioActualId());
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
        verificarPropietario(actual, usuario);
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

    // ---------- ayudas ----------

    /** Un mesero solo trabaja con sus propios pedidos; la cocina y el administrador con todos. */
    private void verificarPropietario(Pedido pedido, Usuario actor) {
        if (TipoRol.de(actor.getRol().getNombreRol()) == TipoRol.MESERO
                && !pedido.getMesero().getIdUsuario().equals(actor.getIdUsuario())) {
            throw new ProhibidoException("Este pedido pertenece a otro mesero.");
        }
    }

    /** Convierte los errores del DAO en las excepciones de la API (404, 400, 409). */
    private RuntimeException traducir(RuntimeException e, String mensajeGenerico) {
        if (e instanceof NoSuchElementException) {
            return new RecursoNoEncontradoException(e.getMessage());
        }
        if (e instanceof IllegalStateException) {
            return new ConflictoException(e.getMessage());
        }
        if (e instanceof IllegalArgumentException) {
            return new OperacionInvalidaException(e.getMessage());
        }
        return new OperacionInvalidaException(mensajeGenerico);
    }
}
