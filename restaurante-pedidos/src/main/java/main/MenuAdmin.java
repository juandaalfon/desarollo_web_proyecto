package main;

import model.Categoria;
import model.DetallePedido;
import model.HistorialEstado;
import model.Mesa;
import model.Pedido;
import model.Producto;
import model.Rol;
import model.Usuario;

/**
 * Menú de Administración: CRUD completo de cada tabla (el mismo menú genérico de ConsolaCrud).
 * Las contraseñas de los usuarios se cifran con BCrypt al guardarlas.
 */
public class MenuAdmin {

    private static final Object[][] TABLAS = {
        {"Rol", Rol.class},
        {"Usuario", Usuario.class},
        {"Mesa", Mesa.class},
        {"Categoría", Categoria.class},
        {"Producto", Producto.class},
        {"Pedido", Pedido.class},
        {"Detalle de pedido", DetallePedido.class},
        {"Historial de estado", HistorialEstado.class},
    };

    private final Entrada in;

    public MenuAdmin(Entrada in) {
        this.in = in;
    }

    public void ejecutar() {
        int opcion;
        do {
            System.out.println("ADMINISTRACION - CRUD CON ORM ");
            for (int i = 0; i < TABLAS.length; i++) {
                System.out.println((i + 1) + ". " + TABLAS[i][0]);
            }
            System.out.println("0. Cerrar sesión");
            opcion = in.entero("Elige una tabla: ");

            if (opcion >= 1 && opcion <= TABLAS.length) {
                abrir((String) TABLAS[opcion - 1][0], (Class<?>) TABLAS[opcion - 1][1]);
            } else if (opcion != 0) {
                System.out.println("Opción no válida.");
            }
            System.out.println();
        } while (opcion != 0);
    }

    private <T> void abrir(String titulo, Class<T> clase) {
        new ConsolaCrud<>(titulo, clase, in.scanner()).ejecutar();
    }
}
