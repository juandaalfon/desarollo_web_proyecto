package main;

import dao.GenericDAO;
import model.DetallePedido;
import model.Mesa;
import model.Pedido;
import model.Producto;
import security.Sesion;
import service.DetallePedidoService;
import service.Estados;
import service.PedidoService;
import service.ServicioException;

import java.util.List;

/**
 * Menú del mesero. Aquí están RF-02 a RF-05 usando el teclado:
 *   1. Crear pedido                    (RF-02)
 *   2. Cambiar el pedido de mesa       (RF-03)
 *   3. Agregar producto al pedido      (RF-04)
 *   4. Registrar observación           (RF-05)
 * Las reglas (mesa libre, capacidad, producto disponible, pedido propio...) las valida el servicio.
 */
public class MenuMesero {

    private final Sesion sesion;
    private final Entrada in;
    private final PedidoService pedidos = new PedidoService();
    private final DetallePedidoService detalles = new DetallePedidoService();

    public MenuMesero(Sesion sesion, Entrada in) {
        this.sesion = sesion;
        this.in = in;
    }

    public void ejecutar() {
        int opcion;
        do {
            System.out.println("MESERO: " + sesion.nombre().toUpperCase() + "");
            System.out.println("1. Crear pedido (RF-02)");
            System.out.println("2. Cambiar un pedido de mesa (RF-03)");
            System.out.println("3. Agregar producto a un pedido (RF-04)");
            System.out.println("4. Registrar observación de un producto (RF-05)");
            System.out.println("5. Quitar un producto del pedido");
            System.out.println("6. Ver un pedido con sus productos");
            System.out.println("7. Ver mis pedidos");
            System.out.println("8. Marcar pedido como ENTREGADO o CANCELADO");
            System.out.println("0. Cerrar sesión");
            opcion = in.entero("Elige una opción: ");

            try {
                switch (opcion) {
                    case 1: crearPedido(); break;
                    case 2: cambiarMesa(); break;
                    case 3: agregarProducto(); break;
                    case 4: registrarObservacion(); break;
                    case 5: quitarProducto(); break;
                    case 6: verPedido(); break;
                    case 7: misPedidos(); break;
                    case 8: cambiarEstado(); break;
                    case 0: break;
                    default: System.out.println("Opción no válida."); break;
                }
            } catch (ServicioException e) {
                System.out.println("No se pudo: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Error inesperado: " + e);
            }
            System.out.println();
        } while (opcion != 0);
    }

    // ------------------------------------------------------------------ opciones

    private void crearPedido() {
        mostrarMesasLibres();
        int idMesa = in.entero("Id de la mesa: ");
        int personas = in.entero("Número de personas: ");
        Pedido p = pedidos.crear(idMesa, personas, sesion);
        System.out.println("Pedido creado: " + p);
    }

    private void cambiarMesa() {
        mostrarMesasLibres();
        int idPedido = in.entero("Id del pedido: ");
        int idMesa = in.entero("Id de la nueva mesa: ");
        Pedido p = pedidos.asignarMesa(idPedido, idMesa, sesion);
        System.out.println("Pedido asignado a la mesa " + p.getMesa().getNumero() + ": " + p);
    }

    private void agregarProducto() {
        mostrarProductosDisponibles();
        int idPedido = in.entero("Id del pedido: ");
        int idProducto = in.entero("Id del producto: ");
        int cantidad = in.entero("Cantidad: ");
        String notas = in.texto("Observación para cocina (Enter si no hay): ");
        DetallePedido d = detalles.agregar(idPedido, idProducto, cantidad, notas, sesion);
        System.out.println("Producto agregado: " + d);
    }

    private void registrarObservacion() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listar(idPedido));
        int idDetalle = in.entero("Id del producto del pedido (el número entre corchetes): ");
        String notas = in.texto("Observación (Enter para borrarla): ");
        DetallePedido d = detalles.registrarObservacion(idDetalle, notas, sesion);
        System.out.println("Observación guardada: " + d);
    }

    private void quitarProducto() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listar(idPedido));
        detalles.eliminar(in.entero("Id del producto del pedido a quitar: "), sesion);
        System.out.println("Producto quitado del pedido.");
    }

    private void verPedido() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listar(idPedido));
    }

    private void misPedidos() {
        List<Pedido> mios = pedidos.listar(null).stream()
                .filter(p -> p.getMesero().getIdUsuario().equals(sesion.idUsuario()))
                .toList();
        if (mios.isEmpty()) {
            System.out.println("Todavía no tienes pedidos.");
        }
        for (Pedido p : mios) {
            Vista.pedido(p, detalles.listar(p.getIdPedido()));
        }
    }

    private void cambiarEstado() {
        int idPedido = in.entero("Id del pedido: ");
        String estado = in.texto("Nuevo estado (" + Estados.ENTREGADO + " o " + Estados.CANCELADO + "): ");
        Pedido p = pedidos.cambiarEstado(idPedido, estado, sesion);
        System.out.println("Pedido actualizado: " + p);
    }

    // ------------------------------------------------------------------ ayudas

    private void mostrarMesasLibres() {
        System.out.println("Mesas libres:");
        boolean hay = false;
        for (Mesa m : new GenericDAO<>(Mesa.class).listar()) {
            if (Estados.MESA_LIBRE.equals(Estados.normalizar(m.getEstado()))) {
                System.out.println("   " + m);
                hay = true;
            }
        }
        if (!hay) {
            System.out.println("   (no hay mesas libres)");
        }
    }

    private void mostrarProductosDisponibles() {
        System.out.println("Productos disponibles:");
        for (Producto p : new GenericDAO<>(Producto.class).listar()) {
            if (Boolean.TRUE.equals(p.getDisponible())) {
                System.out.println("   " + p);
            }
        }
    }
}
