package main;

import api.exception.ConflictoException;
import api.exception.NoAutenticadoException;
import api.exception.OperacionInvalidaException;
import api.exception.ProhibidoException;
import api.exception.RecursoNoEncontradoException;
import api.service.AccesoService;
import api.service.DetallePedidoService;
import api.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import model.DetallePedido;
import model.EstadoMesa;
import model.EstadoPedido;
import model.Mesa;
import model.Pedido;
import model.Producto;
import security.Sesion;

import java.util.List;

/**
 * Menú del mesero. Aquí están RF-02 a RF-05 (y RF-08 / RF-09) usando el teclado:
 *   1. Crear pedido                    (RF-02)
 *   2. Cambiar el pedido de mesa       (RF-03)
 *   3. Agregar producto al pedido      (RF-04)
 *   4. Registrar observación           (RF-05)
 *   8. Entregar o cancelar un pedido   (RF-09 / RF-08)
 * Las reglas (mesa libre, capacidad, producto disponible, pedido propio...) las validan los mismos servicios
 * que usa la API.
 */
public class MenuMesero {

    private final Sesion sesion;
    private final Entrada in;
    private final AccesoService acceso = new AccesoService();
    private final PedidoService pedidos = new PedidoService(new ObjectMapper(), acceso);
    private final DetallePedidoService detalles = new DetallePedidoService(new ObjectMapper(), acceso);

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
            System.out.println("8. Marcar pedido como ENTREGADO (RF-09) o CANCELADO (RF-08)");
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
            } catch (RecursoNoEncontradoException | OperacionInvalidaException | ConflictoException
                     | ProhibidoException | NoAutenticadoException e) {
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
        Pedido p = pedidos.crear(idMesa, personas, sesion.idUsuario());
        System.out.println("Pedido creado: " + p);
    }

    private void cambiarMesa() {
        mostrarMesasLibres();
        int idPedido = in.entero("Id del pedido: ");
        int idMesa = in.entero("Id de la nueva mesa: ");
        Pedido p = pedidos.asignarMesa(idPedido, idMesa, sesion.idUsuario());
        System.out.println("Pedido asignado a la mesa " + p.getMesa().getNumero() + ": " + p);
    }

    private void agregarProducto() {
        mostrarProductosDisponibles();
        int idPedido = in.entero("Id del pedido: ");
        int idProducto = in.entero("Id del producto: ");
        int cantidad = in.entero("Cantidad: ");
        String notas = in.texto("Observación para cocina (Enter si no hay): ");
        DetallePedido d = detalles.agregar(idPedido, idProducto, cantidad, notas, sesion.idUsuario());
        System.out.println("Producto agregado: " + d);
    }

    private void registrarObservacion() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listarPorPedido(idPedido));
        int idDetalle = in.entero("Id del producto del pedido (el número entre corchetes): ");
        String notas = in.texto("Observación (Enter para borrarla): ");
        DetallePedido d = detalles.registrarObservacion(idDetalle, notas, sesion.idUsuario());
        System.out.println("Observación guardada: " + d);
    }

    private void quitarProducto() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listarPorPedido(idPedido));
        detalles.eliminar(in.entero("Id del producto del pedido a quitar: "), sesion.idUsuario());
        System.out.println("Producto quitado del pedido.");
    }

    private void verPedido() {
        int idPedido = in.entero("Id del pedido: ");
        Vista.pedido(pedidos.buscar(idPedido), detalles.listarPorPedido(idPedido));
    }

    private void misPedidos() {
        List<Pedido> mios = pedidos.listar().stream()
                .filter(p -> p.getMesero().getIdUsuario().equals(sesion.idUsuario()))
                .toList();
        if (mios.isEmpty()) {
            System.out.println("Todavía no tienes pedidos.");
        }
        for (Pedido p : mios) {
            Vista.pedido(p, detalles.listarPorPedido(p.getIdPedido()));
        }
    }

    private void cambiarEstado() {
        int idPedido = in.entero("Id del pedido: ");
        String estado = in.texto("Nuevo estado (" + EstadoPedido.ENTREGADO + " o " + EstadoPedido.CANCELADO + "): ");
        Pedido p = pedidos.cambiarEstado(idPedido, estado, sesion.idUsuario());
        System.out.println("Pedido actualizado: " + p);
    }

    // ------------------------------------------------------------------ ayudas

    private void mostrarMesasLibres() {
        System.out.println("Mesas libres:");
        boolean hay = false;
        for (Mesa m : new GenericDAO<>(Mesa.class).listar()) {
            if (EstadoMesa.LIBRE.name().equalsIgnoreCase(m.getEstado())) {
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
