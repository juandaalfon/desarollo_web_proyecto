package main;

import api.dto.Dtos.ItemCocinaDTO;
import api.dto.Dtos.PedidoCocinaDTO;
import api.exception.ConflictoException;
import api.exception.NoAutenticadoException;
import api.exception.OperacionInvalidaException;
import api.exception.ProhibidoException;
import api.exception.RecursoNoEncontradoException;
import api.service.AccesoService;
import api.service.DetallePedidoService;
import api.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.EstadoPedido;
import model.Pedido;
import security.Sesion;

import java.util.List;

/**
 * Menú de cocina: ve la cola de pendientes con las observaciones de cada producto (RF-06 / RF-05),
 * los pedidos activos, y los avanza de estado (PENDIENTE -> EN_PREPARACION -> LISTO) (RF-07).
 */
public class MenuCocina {

    private final Sesion sesion;
    private final Entrada in;
    private final AccesoService acceso = new AccesoService();
    private final PedidoService pedidos = new PedidoService(new ObjectMapper(), acceso);
    private final DetallePedidoService detalles = new DetallePedidoService(new ObjectMapper(), acceso);

    public MenuCocina(Sesion sesion, Entrada in) {
        this.sesion = sesion;
        this.in = in;
    }

    public void ejecutar() {
        int opcion;
        do {
            System.out.println("COCINA: " + sesion.nombre().toUpperCase() + "");
            System.out.println("1. Cola de pedidos PENDIENTES, el más antiguo primero (RF-06)");
            System.out.println("2. Ver pedidos pendientes, en preparación y listos");
            System.out.println("3. Cambiar estado de un pedido (EN_PREPARACION o LISTO) (RF-07)");
            System.out.println("0. Cerrar sesión");
            opcion = in.entero("Elige una opción: ");

            try {
                switch (opcion) {
                    case 1: verCola(); break;
                    case 2: verPedidosActivos(); break;
                    case 3: cambiarEstado(); break;
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

    private void verCola() {
        List<PedidoCocinaDTO> cola = pedidos.colaCocina(sesion.idUsuario());
        if (cola.isEmpty()) {
            System.out.println("No hay pedidos pendientes.");
        }
        for (PedidoCocinaDTO p : cola) {
            System.out.println("--------------------------------------------------");
            System.out.println("Pedido #" + p.idPedido() + " | Mesa " + p.numeroMesa() + " | Personas: " + p.numPersonas()
                    + " | Mesero: " + p.mesero() + " | Esperando: " + p.minutosEnEspera() + " min");
            for (ItemCocinaDTO i : p.items()) {
                System.out.println("   [" + i.idDetalle() + "] " + i.cantidad() + " x " + i.producto());
                if (i.notas() != null && !i.notas().isBlank()) {
                    System.out.println("        Observación: " + i.notas());
                }
            }
        }
    }

    private void verPedidosActivos() {
        List<Pedido> activos = pedidos.listar().stream()
                .filter(p -> {
                    EstadoPedido e = EstadoPedido.desdeTexto(p.getEstado());
                    return !e.esFinal();
                })
                .toList();
        if (activos.isEmpty()) {
            System.out.println("No hay pedidos activos.");
        }
        for (Pedido p : activos) {
            Vista.pedido(p, detalles.listarPorPedido(p.getIdPedido()));
        }
    }

    private void cambiarEstado() {
        int idPedido = in.entero("Id del pedido: ");
        String estado = in.texto("Nuevo estado (" + EstadoPedido.EN_PREPARACION + " o " + EstadoPedido.LISTO + "): ");
        Pedido p = pedidos.cambiarEstado(idPedido, estado, sesion.idUsuario());
        System.out.println("Pedido actualizado: " + p);
    }
}
