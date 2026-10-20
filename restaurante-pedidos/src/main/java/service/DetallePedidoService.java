package service;

import jakarta.persistence.EntityManager;
import model.DetallePedido;
import model.Pedido;
import model.Producto;
import security.Sesion;
import security.TipoRol;
import util.JPAUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-04 (productos del pedido) y RF-05 (observaciones del producto, columna "notas").
 *
 * Reglas:
 *  - Solo se agregan productos disponibles; el precio se copia del producto en ese momento
 *    y el subtotal se calcula como precio x cantidad.
 *  - Los productos de un pedido solo se modifican mientras esté PENDIENTE o EN_PREPARACION.
 *  - Un mesero solo modifica pedidos propios; la cocina solo consulta.
 */
public class DetallePedidoService {

    private static final int MAX_NOTAS = 255;

    // ---------------------------------------------------------------- consultas

    /** Productos de un pedido; con idPedido en null devuelve todos los detalles. */
    public List<DetallePedido> listar(Integer idPedido) {
        return JPAUtil.enLectura(em -> {
            if (idPedido == null) {
                return em.createQuery("SELECT d FROM DetallePedido d ORDER BY d.idDetalle", DetallePedido.class)
                        .getResultList();
            }
            return em.createQuery(
                            "SELECT d FROM DetallePedido d WHERE d.pedido.idPedido = ?1 ORDER BY d.idDetalle",
                            DetallePedido.class)
                    .setParameter(1, idPedido)
                    .getResultList();
        });
    }

    public DetallePedido buscar(Integer idDetalle) {
        DetallePedido d = JPAUtil.enLectura(em -> em.find(DetallePedido.class, idDetalle));
        if (d == null) {
            throw ServicioException.noEncontrado("El detalle " + idDetalle + " no existe.");
        }
        return d;
    }

    // ------------------------------------------------------------------- RF-04

    /** Agrega un producto al pedido. "notas" (observación) es opcional: RF-05. */
    public DetallePedido agregar(Integer idPedido, Integer idProducto, Integer cantidad, String notas,
                                 Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);
        if (idPedido == null || idProducto == null) {
            throw ServicioException.invalida("El pedido y el producto son obligatorios.");
        }
        validarCantidad(cantidad);
        String observacion = limpiarNotas(notas);

        return JPAUtil.enTransaccion(em -> {
            Pedido pedido = em.find(Pedido.class, idPedido);
            if (pedido == null) {
                throw ServicioException.noEncontrado("El pedido " + idPedido + " no existe.");
            }
            verificarEditable(pedido, sesion);

            Producto producto = em.find(Producto.class, idProducto);
            if (producto == null) {
                throw ServicioException.noEncontrado("El producto " + idProducto + " no existe.");
            }
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw ServicioException.conflicto("El producto '" + producto.getNombre() + "' no está disponible.");
            }

            DetallePedido d = new DetallePedido();
            d.setPedido(pedido);
            d.setProducto(producto);
            d.setCantidad(cantidad);
            d.setNotas(observacion);
            d.setPrecioUnitario(producto.getPrecio());
            d.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            em.persist(d);

            pedido.setFechaUltimaActualizacion(LocalDateTime.now());
            return d;
        });
    }

    /**
     * Cambia la cantidad y/o las notas de un producto del pedido.
     * @param cantidad  nueva cantidad, o null para no cambiarla
     * @param tocarNotas true si se debe reemplazar la observación (con notas en null o vacío se borra)
     */
    public DetallePedido actualizar(Integer idDetalle, Integer cantidad, boolean tocarNotas, String notas,
                                    Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);
        if (cantidad != null) {
            validarCantidad(cantidad);
        }
        String observacion = tocarNotas ? limpiarNotas(notas) : null;

        return JPAUtil.enTransaccion(em -> {
            DetallePedido d = cargar(em, idDetalle);
            verificarEditable(d.getPedido(), sesion);

            if (cantidad != null) {
                d.setCantidad(cantidad);
                d.setSubtotal(d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad)));
            }
            if (tocarNotas) {
                d.setNotas(observacion);
            }
            d.getPedido().setFechaUltimaActualizacion(LocalDateTime.now());
            return d;
        });
    }

    // ------------------------------------------------------------------- RF-05

    /** Registra, cambia o borra (con texto vacío) la observación de un producto del pedido. */
    public DetallePedido registrarObservacion(Integer idDetalle, String notas, Sesion sesion) {
        return actualizar(idDetalle, null, true, notas, sesion);
    }

    // ---------------------------------------------------------------- eliminar

    public void eliminar(Integer idDetalle, Sesion sesion) {
        sesion.exigirRol(TipoRol.MESERO, TipoRol.ADMINISTRACION);

        JPAUtil.enTransaccion(em -> {
            DetallePedido d = cargar(em, idDetalle);
            verificarEditable(d.getPedido(), sesion);
            d.getPedido().setFechaUltimaActualizacion(LocalDateTime.now());
            em.remove(d);
            return null;
        });
    }

    // ------------------------------------------------------------------ ayudas

    private DetallePedido cargar(EntityManager em, Integer idDetalle) {
        DetallePedido d = idDetalle == null ? null : em.find(DetallePedido.class, idDetalle);
        if (d == null) {
            throw ServicioException.noEncontrado("El detalle " + idDetalle + " no existe.");
        }
        return d;
    }

    private void verificarEditable(Pedido pedido, Sesion sesion) {
        String estado = Estados.normalizar(pedido.getEstado());
        if (!Estados.PENDIENTE.equals(estado) && !Estados.EN_PREPARACION.equals(estado)) {
            throw ServicioException.conflicto("No se pueden modificar los productos de un pedido en estado "
                    + pedido.getEstado() + ".");
        }
        if (sesion.tipoRol() == TipoRol.MESERO
                && !pedido.getMesero().getIdUsuario().equals(sesion.idUsuario())) {
            throw ServicioException.prohibido("Este pedido pertenece a otro mesero.");
        }
    }

    private void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw ServicioException.invalida("La cantidad debe ser al menos 1.");
        }
    }

    private String limpiarNotas(String notas) {
        if (notas == null || notas.isBlank()) {
            return null;
        }
        String limpia = notas.trim();
        if (limpia.length() > MAX_NOTAS) {
            throw ServicioException.invalida("La observación puede tener máximo " + MAX_NOTAS + " caracteres.");
        }
        return limpia;
    }
}
