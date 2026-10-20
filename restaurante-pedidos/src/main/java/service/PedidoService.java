package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import model.DetallePedido;
import model.HistorialEstado;
import model.Mesa;
import model.Pedido;
import model.Usuario;
import security.Sesion;
import security.TipoRol;
import util.JPAUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-02 (gestión de pedidos) y RF-03 (asignación de mesa).
 *
 * Reglas:
 *  - Un pedido nace PENDIENTE, a nombre del mesero que inició sesión, y siempre tiene una mesa.
 *  - Una mesa solo se asigna si está LIBRE y le alcanza la capacidad; al asignarla pasa a OCUPADA.
 *  - Al cambiar de mesa, la anterior queda LIBRE. Al terminar el pedido (ENTREGADO/CANCELADO) la mesa queda LIBRE.
 *  - Cada cambio de estado se guarda en historial_estado.
 *  - Un mesero solo modifica sus propios pedidos; la administración puede con todos.
 */
public class PedidoService {

    // ---------------------------------------------------------------- consultas

    public List<Pedido> listar(String estado) {
        return JPAUtil.enLectura(em -> {
            if (estado == null || estado.isBlank()) {
                return em.createQuery("SELECT p FROM Pedido p ORDER BY p.fechaCreacion DESC", Pedido.class)
                        .getResultList();
            }
            return em.createQuery("SELECT p FROM Pedido p WHERE p.estado = ?1 ORDER BY p.fechaCreacion DESC",
                            Pedido.class)
                    .setParameter(1, Estados.normalizar(estado))
                    .getResultList();
        });
    }

    public Pedido buscar(Integer id) {
        Pedido p = JPAUtil.enLectura(em -> em.find(Pedido.class, id));
        if (p == null) {
            throw ServicioException.noEncontrado("El pedido " + id + " no existe.");
        }
        return p;
    }

    /** Pedido + sus productos (con observaciones) + total. Lo usan el mesero y la cocina. */
    public Map<String, Object> resumen(Integer id) {
        Pedido pedido = buscar(id);
        List<DetallePedido> detalles = new DetallePedidoService().listar(id);
        BigDecimal total = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("pedido", pedido);
        resumen.put("detalles", detalles);
        resumen.put("total", total);
        return resumen;
    }

    // ------------------------------------------------------------------- RF-02

    /** Crea un pedido nuevo para una mesa libre. El mesero es quien tiene la sesión iniciada. */
    public Pedido crear(Integer idMesa, Integer numPersonas, Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);
        if (idMesa == null) {
            throw ServicioException.invalida("La mesa es obligatoria.");
        }
        if (numPersonas == null || numPersonas < 1) {
            throw ServicioException.invalida("El número de personas debe ser al menos 1.");
        }

        return JPAUtil.enTransaccion(em -> {
            Mesa mesa = bloquearMesa(em, idMesa);
            validarMesaDisponible(mesa, numPersonas);

            Usuario mesero = em.find(Usuario.class, sesion.idUsuario());
            LocalDateTime ahora = LocalDateTime.now();

            Pedido pedido = new Pedido();
            pedido.setMesa(mesa);
            pedido.setMesero(mesero);
            pedido.setNumPersonas(numPersonas);
            pedido.setEstado(Estados.PENDIENTE);
            pedido.setFechaCreacion(ahora);
            pedido.setFechaUltimaActualizacion(ahora);
            em.persist(pedido);

            mesa.setEstado(Estados.MESA_OCUPADA);
            registrarHistorial(em, pedido, Estados.PENDIENTE, mesero, ahora);
            return pedido;
        });
    }

    /**
     * Cambia el número de personas y/o la mesa (RF-03). Los parámetros en null no se modifican.
     */
    public Pedido actualizar(Integer idPedido, Integer idMesa, Integer numPersonas, Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);
        if (numPersonas != null && numPersonas < 1) {
            throw ServicioException.invalida("El número de personas debe ser al menos 1.");
        }

        return JPAUtil.enTransaccion(em -> {
            Pedido pedido = cargar(em, idPedido);
            verificarModificable(pedido, sesion);

            int personas = numPersonas != null ? numPersonas : pedido.getNumPersonas();

            if (idMesa != null && !idMesa.equals(pedido.getMesa().getIdMesa())) {
                Mesa nueva = bloquearMesa(em, idMesa);
                validarMesaDisponible(nueva, personas);
                pedido.getMesa().setEstado(Estados.MESA_LIBRE); // se libera la mesa anterior
                nueva.setEstado(Estados.MESA_OCUPADA);
                pedido.setMesa(nueva);
            } else if (personas > pedido.getMesa().getCapacidad()) {
                throw ServicioException.invalida("La mesa " + pedido.getMesa().getNumero()
                        + " tiene capacidad para " + pedido.getMesa().getCapacidad() + " personas.");
            }

            pedido.setNumPersonas(personas);
            pedido.setFechaUltimaActualizacion(LocalDateTime.now());
            return pedido;
        });
    }

    /** RF-03: asigna el pedido a otra mesa. */
    public Pedido asignarMesa(Integer idPedido, Integer idMesa, Sesion sesion) {
        if (idMesa == null) {
            throw ServicioException.invalida("La mesa es obligatoria.");
        }
        return actualizar(idPedido, idMesa, null, sesion);
    }

    /**
     * Cambia el estado del pedido siguiendo el flujo PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
     * (o CANCELADO). Cocina solo pasa a EN_PREPARACION y LISTO; el mesero solo a ENTREGADO y CANCELADO.
     */
    public Pedido cambiarEstado(Integer idPedido, String estadoNuevo, Sesion sesion) {
        String nuevo = Estados.normalizar(estadoNuevo);
        if (nuevo == null || !Estados.existe(nuevo)) {
            throw ServicioException.invalida("Estado no válido. Usa: PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO o CANCELADO.");
        }

        return JPAUtil.enTransaccion(em -> {
            Pedido pedido = cargar(em, idPedido);
            String actual = pedido.getEstado();

            if (!Estados.transicionValida(actual, nuevo)) {
                throw ServicioException.conflicto("No se puede pasar el pedido de " + actual + " a " + nuevo + ".");
            }
            validarRolParaEstado(pedido, nuevo, sesion);

            if (Estados.EN_PREPARACION.equals(nuevo)) {
                Long productos = em.createQuery(
                                "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.idPedido = ?1", Long.class)
                        .setParameter(1, idPedido)
                        .getSingleResult();
                if (productos == 0) {
                    throw ServicioException.conflicto("El pedido no tiene productos; agrega al menos uno antes de enviarlo a cocina.");
                }
            }

            LocalDateTime ahora = LocalDateTime.now();
            pedido.setEstado(nuevo);
            pedido.setFechaUltimaActualizacion(ahora);
            registrarHistorial(em, pedido, nuevo, em.find(Usuario.class, sesion.idUsuario()), ahora);

            if (Estados.esFinal(nuevo)) {
                pedido.getMesa().setEstado(Estados.MESA_LIBRE);
            }
            return pedido;
        });
    }

    /** Solo se pueden eliminar pedidos PENDIENTE o CANCELADO. Borra también sus productos e historial. */
    public void eliminar(Integer idPedido, Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);

        JPAUtil.enTransaccion(em -> {
            Pedido pedido = cargar(em, idPedido);
            verificarPropietario(pedido, sesion);

            String estado = Estados.normalizar(pedido.getEstado());
            if (!Estados.PENDIENTE.equals(estado) && !Estados.CANCELADO.equals(estado)) {
                throw ServicioException.conflicto("Solo se pueden eliminar pedidos PENDIENTE o CANCELADO.");
            }

            em.createQuery("DELETE FROM DetallePedido d WHERE d.pedido.idPedido = ?1")
                    .setParameter(1, idPedido).executeUpdate();
            em.createQuery("DELETE FROM HistorialEstado h WHERE h.pedido.idPedido = ?1")
                    .setParameter(1, idPedido).executeUpdate();

            if (!Estados.esFinal(estado)) {
                pedido.getMesa().setEstado(Estados.MESA_LIBRE);
            }
            em.remove(pedido);
            return null;
        });
    }

    // ------------------------------------------------------------------ ayudas

    private Pedido cargar(EntityManager em, Integer idPedido) {
        Pedido pedido = idPedido == null ? null : em.find(Pedido.class, idPedido);
        if (pedido == null) {
            throw ServicioException.noEncontrado("El pedido " + idPedido + " no existe.");
        }
        return pedido;
    }

    /** Bloquea la fila de la mesa para que dos meseros no tomen la misma mesa al mismo tiempo. */
    private Mesa bloquearMesa(EntityManager em, Integer idMesa) {
        Mesa mesa = em.find(Mesa.class, idMesa, LockModeType.PESSIMISTIC_WRITE);
        if (mesa == null) {
            throw ServicioException.noEncontrado("La mesa " + idMesa + " no existe.");
        }
        return mesa;
    }

    private void validarMesaDisponible(Mesa mesa, int personas) {
        if (!Estados.MESA_LIBRE.equals(Estados.normalizar(mesa.getEstado()))) {
            throw ServicioException.conflicto("La mesa " + mesa.getNumero() + " no está libre (estado: "
                    + mesa.getEstado() + ").");
        }
        if (personas > mesa.getCapacidad()) {
            throw ServicioException.invalida("La mesa " + mesa.getNumero() + " tiene capacidad para "
                    + mesa.getCapacidad() + " personas.");
        }
    }

    private void verificarModificable(Pedido pedido, Sesion sesion) {
        if (Estados.esFinal(pedido.getEstado())) {
            throw ServicioException.conflicto("El pedido ya está " + pedido.getEstado() + " y no se puede modificar.");
        }
        verificarPropietario(pedido, sesion);
    }

    private void verificarPropietario(Pedido pedido, Sesion sesion) {
        if (sesion.tipoRol() == TipoRol.MESERO
                && !pedido.getMesero().getIdUsuario().equals(sesion.idUsuario())) {
            throw ServicioException.prohibido("Este pedido pertenece a otro mesero.");
        }
    }

    private void validarRolParaEstado(Pedido pedido, String nuevo, Sesion sesion) {
        switch (sesion.tipoRol()) {
            case ADMINISTRACION:
                return;
            case COCINA:
                if (Estados.EN_PREPARACION.equals(nuevo) || Estados.LISTO.equals(nuevo)) {
                    return;
                }
                throw ServicioException.prohibido("La cocina solo puede pasar pedidos a EN_PREPARACION o LISTO.");
            case MESERO:
                verificarPropietario(pedido, sesion);
                if (Estados.ENTREGADO.equals(nuevo) || Estados.CANCELADO.equals(nuevo)) {
                    return;
                }
                throw ServicioException.prohibido("El mesero solo puede pasar pedidos a ENTREGADO o CANCELADO.");
            default:
                throw ServicioException.prohibido("Rol sin permiso para cambiar el estado.");
        }
    }

    private void registrarHistorial(EntityManager em, Pedido pedido, String estado, Usuario usuario,
                                    LocalDateTime fecha) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstado(estado);
        h.setFechaHora(fecha);
        h.setUsuario(usuario);
        em.persist(h);
    }
}
