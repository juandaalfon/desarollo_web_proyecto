package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import model.DetallePedido;
import model.EstadoPedido;
import model.EstadoMesa;
import model.HistorialEstado;
import model.Mesa;
import model.Pedido;
import model.Usuario;
import util.JPAUtil;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Consultas y operaciones propias de los pedidos que el GenericDAO no cubre.
 * Las que tocan varias tablas (pedido + historial) se hacen en UNA sola transacción:
 * o se guarda todo o no se guarda nada.
 */
public class PedidoDAO {

    /** Pedidos en un estado, del más antiguo al más nuevo (cola FIFO). */
    public List<Pedido> listarPorEstado(EstadoPedido estado) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Pedido p WHERE UPPER(p.estado) = :estado "
                                    + "ORDER BY p.fechaCreacion ASC, p.idPedido ASC", Pedido.class)
                    .setParameter("estado", estado.name())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<DetallePedido> detallesDePedido(Integer idPedido) {
        return detallesDePedidos(Collections.singletonList(idPedido));
    }

    /** Detalles de varios pedidos en una sola consulta. */
    public List<DetallePedido> detallesDePedidos(Collection<Integer> idsPedido) {
        if (idsPedido.isEmpty()) {
            return Collections.emptyList();
        }
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT d FROM DetallePedido d WHERE d.pedido.idPedido IN :ids ORDER BY d.idDetalle ASC",
                            DetallePedido.class)
                    .setParameter("ids", idsPedido)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Historial de un pedido en orden cronológico. */
    public List<HistorialEstado> historialDePedido(Integer idPedido) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT h FROM HistorialEstado h WHERE h.pedido.idPedido = :id "
                                    + "ORDER BY h.fechaHora ASC, h.idHistorial ASC", HistorialEstado.class)
                    .setParameter("id", idPedido)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * RF-02 / RF-03: crea el pedido y registra su primer estado (PENDIENTE) en el historial, en una transacción.
     * La mesa se bloquea para que dos meseros no la tomen a la vez: debe estar LIBRE y alcanzar para las personas;
     * al crear el pedido pasa a OCUPADA.
     *
     * @throws IllegalStateException    si la mesa no está libre
     * @throws IllegalArgumentException si la capacidad de la mesa no alcanza
     */
    public Pedido crearConHistorial(Pedido pedido) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Mesa mesa = bloquearMesa(em, pedido.getMesa().getIdMesa());
            validarMesaDisponible(mesa, pedido.getNumPersonas());
            mesa.setEstado(EstadoMesa.OCUPADA.name());
            pedido.setMesa(mesa);
            em.persist(pedido);
            em.persist(nuevoHistorial(pedido, EstadoPedido.PENDIENTE, pedido.getFechaCreacion(), pedido.getMesero()));
            tx.commit();
            return pedido;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Cambia el estado, actualiza la fecha de última actualización y registra el cambio en el historial,
     * todo en una transacción. Vuelve a validar la transición con el pedido bloqueado para evitar que dos
     * personas lo cambien a la vez.
     *
     * @throws NoSuchElementException si el pedido no existe
     * @throws IllegalStateException  si la transición no está permitida
     */
    public Pedido cambiarEstado(Integer idPedido, EstadoPedido destino, Usuario usuario) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new NoSuchElementException("Pedido con id " + idPedido + " no existe.");
            }
            EstadoPedido actual;
            try {
                actual = EstadoPedido.desdeTexto(pedido.getEstado());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("El pedido tiene un estado no reconocido: '" + pedido.getEstado() + "'.");
            }
            if (!actual.puedePasarA(destino)) {
                throw new IllegalStateException(actual.mensajeTransicion(destino));
            }
            if (destino == EstadoPedido.EN_PREPARACION) {
                Long productos = em.createQuery(
                                "SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.idPedido = :id", Long.class)
                        .setParameter("id", idPedido)
                        .getSingleResult();
                if (productos == 0) {
                    throw new IllegalStateException(
                            "El pedido no tiene productos; agrega al menos uno antes de enviarlo a cocina.");
                }
            }
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            pedido.setEstado(destino.name());
            pedido.setFechaUltimaActualizacion(ahora);
            em.persist(nuevoHistorial(pedido, destino, ahora, usuario));
            if (destino.esFinal()) {
                pedido.getMesa().setEstado(EstadoMesa.LIBRE.name()); // ENTREGADO o CANCELADO: la mesa queda libre
            }
            tx.commit();
            return pedido;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * RF-03: cambia la mesa y/o el número de personas del pedido (los parámetros null no se modifican).
     * La mesa nueva debe estar LIBRE y alcanzar para las personas; la anterior queda LIBRE.
     *
     * @throws NoSuchElementException   si el pedido o la mesa no existen
     * @throws IllegalStateException    si el pedido ya terminó o la mesa nueva no está libre
     * @throws IllegalArgumentException si la capacidad de la mesa no alcanza
     */
    public Pedido cambiarMesaYPersonas(Integer idPedido, Integer idMesa, Integer numPersonas) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new NoSuchElementException("Pedido con id " + idPedido + " no existe.");
            }
            if (estadoDe(pedido).esFinal()) {
                throw new IllegalStateException("El pedido ya está " + pedido.getEstado() + " y no se puede modificar.");
            }
            int personas = numPersonas != null ? numPersonas : pedido.getNumPersonas();

            if (idMesa != null && !idMesa.equals(pedido.getMesa().getIdMesa())) {
                Mesa nueva = bloquearMesa(em, idMesa);
                validarMesaDisponible(nueva, personas);
                pedido.getMesa().setEstado(EstadoMesa.LIBRE.name()); // se libera la mesa anterior
                nueva.setEstado(EstadoMesa.OCUPADA.name());
                pedido.setMesa(nueva);
            } else if (personas > pedido.getMesa().getCapacidad()) {
                throw new IllegalArgumentException("La mesa " + pedido.getMesa().getNumero()
                        + " tiene capacidad para " + pedido.getMesa().getCapacidad() + " personas.");
            }
            pedido.setNumPersonas(personas);
            pedido.setFechaUltimaActualizacion(LocalDateTime.now().withNano(0));
            tx.commit();
            return pedido;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Elimina un pedido PENDIENTE o CANCELADO junto con sus productos y su historial. Si el pedido seguía
     * activo, la mesa queda LIBRE.
     *
     * @throws NoSuchElementException si el pedido no existe
     * @throws IllegalStateException  si el pedido no está PENDIENTE ni CANCELADO
     */
    public void eliminarConDependencias(Integer idPedido) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new NoSuchElementException("Pedido con id " + idPedido + " no existe.");
            }
            EstadoPedido estado = estadoDe(pedido);
            if (estado != EstadoPedido.PENDIENTE && estado != EstadoPedido.CANCELADO) {
                throw new IllegalStateException("Solo se pueden eliminar pedidos PENDIENTE o CANCELADO.");
            }
            em.createQuery("DELETE FROM DetallePedido d WHERE d.pedido.idPedido = :id")
                    .setParameter("id", idPedido).executeUpdate();
            em.createQuery("DELETE FROM HistorialEstado h WHERE h.pedido.idPedido = :id")
                    .setParameter("id", idPedido).executeUpdate();
            if (!estado.esFinal()) {
                pedido.getMesa().setEstado(EstadoMesa.LIBRE.name());
            }
            em.remove(pedido);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    // ---------- ayudas ----------

    private EstadoPedido estadoDe(Pedido pedido) {
        try {
            return EstadoPedido.desdeTexto(pedido.getEstado());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("El pedido tiene un estado no reconocido: '" + pedido.getEstado() + "'.");
        }
    }

    /** Bloquea la fila de la mesa hasta que termine la transacción. */
    private Mesa bloquearMesa(EntityManager em, Integer idMesa) {
        Mesa mesa = idMesa == null ? null : em.find(Mesa.class, idMesa, LockModeType.PESSIMISTIC_WRITE);
        if (mesa == null) {
            throw new NoSuchElementException("La mesa con id " + idMesa + " no existe.");
        }
        return mesa;
    }

    private void validarMesaDisponible(Mesa mesa, int personas) {
        EstadoMesa estado;
        try {
            estado = EstadoMesa.desdeTexto(mesa.getEstado());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " tiene un estado no reconocido: '"
                    + mesa.getEstado() + "'.");
        }
        if (estado != EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " no está libre (estado: " + estado + ").");
        }
        if (personas > mesa.getCapacidad()) {
            throw new IllegalArgumentException("La mesa " + mesa.getNumero() + " tiene capacidad para "
                    + mesa.getCapacidad() + " personas.");
        }
    }

    private HistorialEstado nuevoHistorial(Pedido pedido, EstadoPedido estado, LocalDateTime fecha, Usuario usuario) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstado(estado.name());
        h.setFechaHora(fecha);
        h.setUsuario(usuario);
        return h;
    }
}
