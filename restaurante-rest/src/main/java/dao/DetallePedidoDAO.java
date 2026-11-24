package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import model.DetallePedido;
import model.EstadoPedido;
import model.Pedido;
import model.Producto;
import util.JPAUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Operaciones de los productos de un pedido (RF-04) y sus observaciones (RF-05) que tocan varias tablas.
 * Cada una corre en UNA transacción con el pedido bloqueado, así un producto no se agrega mientras la cocina
 * (u otro usuario) cambia el estado del pedido.
 *
 * Los errores se avisan con excepciones estándar que el servicio convierte en 404 / 409:
 *   NoSuchElementException  -> el pedido, el producto o el detalle no existe
 *   IllegalStateException   -> el pedido ya no se puede modificar o el producto no está disponible
 */
public class DetallePedidoDAO {

    /** Agrega un producto: el precio se copia del producto y el subtotal es precio x cantidad. */
    public DetallePedido agregar(Integer idPedido, Integer idProducto, int cantidad, String notas) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Pedido pedido = pedidoEditable(em, idPedido);
            Producto producto = em.find(Producto.class, idProducto);
            if (producto == null) {
                throw new NoSuchElementException("El producto " + idProducto + " no existe.");
            }
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw new IllegalStateException("El producto '" + producto.getNombre() + "' no está disponible.");
            }
            DetallePedido d = new DetallePedido();
            d.setPedido(pedido);
            d.setProducto(producto);
            d.setCantidad(cantidad);
            d.setNotas(notas);
            d.setPrecioUnitario(producto.getPrecio());
            d.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            em.persist(d);
            pedido.setFechaUltimaActualizacion(LocalDateTime.now().withNano(0));
            tx.commit();
            return d;
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
     * Cambia la cantidad (null = no cambia) y/o la observación (solo si tocarNotas; null la borra).
     * El subtotal se recalcula con el precio que tenía el producto al agregarlo.
     */
    public DetallePedido actualizar(Integer idDetalle, Integer cantidad, boolean tocarNotas, String notas) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            DetallePedido d = cargar(em, idDetalle);
            Pedido pedido = pedidoEditable(em, d.getPedido().getIdPedido());
            if (cantidad != null) {
                d.setCantidad(cantidad);
                d.setSubtotal(d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad)));
            }
            if (tocarNotas) {
                d.setNotas(notas);
            }
            pedido.setFechaUltimaActualizacion(LocalDateTime.now().withNano(0));
            tx.commit();
            return d;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void eliminar(Integer idDetalle) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            DetallePedido d = cargar(em, idDetalle);
            Pedido pedido = pedidoEditable(em, d.getPedido().getIdPedido());
            em.remove(d);
            pedido.setFechaUltimaActualizacion(LocalDateTime.now().withNano(0));
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

    private DetallePedido cargar(EntityManager em, Integer idDetalle) {
        DetallePedido d = idDetalle == null ? null : em.find(DetallePedido.class, idDetalle);
        if (d == null) {
            throw new NoSuchElementException("El detalle " + idDetalle + " no existe.");
        }
        return d;
    }

    /** Pedido bloqueado que todavía admite cambios en sus productos: PENDIENTE o EN_PREPARACION. */
    private Pedido pedidoEditable(EntityManager em, Integer idPedido) {
        Pedido pedido = idPedido == null ? null : em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
        if (pedido == null) {
            throw new NoSuchElementException("El pedido " + idPedido + " no existe.");
        }
        EstadoPedido estado;
        try {
            estado = EstadoPedido.desdeTexto(pedido.getEstado());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("El pedido tiene un estado no reconocido: '" + pedido.getEstado() + "'.");
        }
        if (estado != EstadoPedido.PENDIENTE && estado != EstadoPedido.EN_PREPARACION) {
            throw new IllegalStateException(
                    "No se pueden modificar los productos de un pedido en estado " + pedido.getEstado() + ".");
        }
        return pedido;
    }
}
