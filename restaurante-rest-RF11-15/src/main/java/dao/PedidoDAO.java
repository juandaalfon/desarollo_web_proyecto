package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import model.DetallePedido;
import model.EstadoPedido;
import model.HistorialEstado;
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

    /** Crea el pedido y registra su primer estado (PENDIENTE) en el historial, en una transacción. */
    public Pedido crearConHistorial(Pedido pedido) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
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
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            pedido.setEstado(destino.name());
            pedido.setFechaUltimaActualizacion(ahora);
            em.persist(nuevoHistorial(pedido, destino, ahora, usuario));
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

    private HistorialEstado nuevoHistorial(Pedido pedido, EstadoPedido estado, LocalDateTime fecha, Usuario usuario) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstado(estado.name());
        h.setFechaHora(fecha);
        h.setUsuario(usuario);
        return h;
    }
}
