package main;

import model.Pedido;
import security.Sesion;
import service.DetallePedidoService;
import service.Estados;
import service.PedidoService;
import service.ServicioException;

import java.util.List;

/**
 * Menú de cocina: ve los pedidos activos con las observaciones de cada producto (RF-05)
 * y los avanza de estado (PENDIENTE -> EN_PREPARACION -> LISTO).
 */
public class MenuCocina {

    private final Sesion sesion;
    private final Entrada in;
    private final PedidoService pedidos = new PedidoService();
    private final DetallePedidoService detalles = new DetallePedidoService();

    public MenuCocina(Sesion sesion, Entrada in) {
        this.sesion = sesion;
        this.in = in;
    }

    public void ejecutar() {
        int opcion;
        do {
            System.out.println("COCINA: " + sesion.nombre().toUpperCase() + "");
            System.out.println("1. Ver pedidos pendientes, en preparación y listos");
            System.out.println("2. Cambiar estado de un pedido (EN_PREPARACION o LISTO)");
            System.out.println("0. Cerrar sesión");
            opcion = in.entero("Elige una opción: ");

            try {
                switch (opcion) {
                    case 1: verPedidosActivos(); break;
                    case 2: cambiarEstado(); break;
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

    private void verPedidosActivos() {
        List<Pedido> activos = pedidos.listar(null).stream()
                .filter(p -> !Estados.esFinal(p.getEstado()))
                .toList();
        if (activos.isEmpty()) {
            System.out.println("No hay pedidos activos.");
        }
        for (Pedido p : activos) {
            Vista.pedido(p, detalles.listar(p.getIdPedido()));
        }
    }

    private void cambiarEstado() {
        int idPedido = in.entero("Id del pedido: ");
        String estado = in.texto("Nuevo estado (" + Estados.EN_PREPARACION + " o " + Estados.LISTO + "): ");
        Pedido p = pedidos.cambiarEstado(idPedido, estado, sesion);
        System.out.println("Pedido actualizado: " + p);
    }
}
