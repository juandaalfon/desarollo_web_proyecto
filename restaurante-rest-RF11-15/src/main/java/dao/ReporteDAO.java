package dao;

import jakarta.persistence.EntityManager;
import model.HistorialEstado;
import util.JPAUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de los reportes (RF-15). Una venta es un pedido que llegó a ENTREGADO;
 * la fecha de la venta es la fecha en que quedó ENTREGADO (según el historial).
 * El rango es [desde, hasta): incluye "desde" y excluye "hasta".
 */
public class ReporteDAO {

    /** Filas: [idPedido, fechaEntrega (LocalDateTime), totalPedido (suma de subtotales)]. */
    public List<Object[]> ventasPorPedido(LocalDateTime desde, LocalDateTime hasta) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT d.pedido.idPedido, h.fechaHora, SUM(d.subtotal) "
                                    + "FROM DetallePedido d, HistorialEstado h "
                                    + "WHERE h.pedido = d.pedido AND h.estado = 'ENTREGADO' "
                                    + "AND h.fechaHora >= :desde AND h.fechaHora < :hasta "
                                    + "GROUP BY d.pedido.idPedido, h.fechaHora", Object[].class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Filas: [idProducto, producto, categoria, unidades vendidas, total vendido], de más a menos vendido. */
    public List<Object[]> productosVendidos(LocalDateTime desde, LocalDateTime hasta) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT pr.idProducto, pr.nombre, c.nombre, SUM(d.cantidad), SUM(d.subtotal) "
                                    + "FROM DetallePedido d JOIN d.producto pr JOIN pr.categoria c, HistorialEstado h "
                                    + "WHERE h.pedido = d.pedido AND h.estado = 'ENTREGADO' "
                                    + "AND h.fechaHora >= :desde AND h.fechaHora < :hasta "
                                    + "GROUP BY pr.idProducto, pr.nombre, c.nombre "
                                    + "ORDER BY SUM(d.cantidad) DESC, pr.nombre ASC", Object[].class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Historial completo de los pedidos entregados en el rango, para calcular tiempos por etapa. */
    public List<HistorialEstado> historialDeEntregados(LocalDateTime desde, LocalDateTime hasta) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery(
                            "SELECT h FROM HistorialEstado h WHERE h.pedido.idPedido IN ("
                                    + "SELECT h2.pedido.idPedido FROM HistorialEstado h2 "
                                    + "WHERE h2.estado = 'ENTREGADO' AND h2.fechaHora >= :desde AND h2.fechaHora < :hasta) "
                                    + "ORDER BY h.pedido.idPedido ASC, h.fechaHora ASC, h.idHistorial ASC",
                            HistorialEstado.class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
