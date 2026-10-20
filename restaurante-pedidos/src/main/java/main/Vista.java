package main;

import model.DetallePedido;
import model.Pedido;

import java.math.BigDecimal;
import java.util.List;

/** Cómo se imprime un pedido con sus productos y observaciones en la consola. */
final class Vista {

    private Vista() {
    }

    static void pedido(Pedido p, List<DetallePedido> detalles) {
        System.out.println("--------------------------------------------------");
        System.out.println("Pedido #" + p.getIdPedido()
                + " | Mesa " + (p.getMesa() != null ? p.getMesa().getNumero() : "-")
                + " | Personas: " + p.getNumPersonas()
                + " | Estado: " + p.getEstado()
                + " | Mesero: " + (p.getMesero() != null ? p.getMesero().getNombre() : "-"));

        if (detalles.isEmpty()) {
            System.out.println("   (sin productos todavía)");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido d : detalles) {
            System.out.println("   [" + d.getIdDetalle() + "] " + d.getCantidad() + " x " + d.getProducto().getNombre()
                    + "  $" + d.getPrecioUnitario() + " c/u = $" + d.getSubtotal());
            if (d.getNotas() != null && !d.getNotas().isBlank()) {
                System.out.println("        Observación: " + d.getNotas());
            }
            total = total.add(d.getSubtotal());
        }
        System.out.println("   TOTAL: $" + total);
    }
}
