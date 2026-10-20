package main;

import util.JPAUtil;

import java.util.Scanner;
import model.Rol;
import model.Usuario;
import model.Mesa;
import model.Categoria;
import model.Producto;
import model.Pedido;
import model.DetallePedido;
import model.HistorialEstado;
import model.Factura;

/**
 * Menú principal: elige la tabla sobre la que se quiere hacer el CRUD (acceso con ORM/Hibernate).
 */
public class Main {

    private static final Object[][] TABLAS = {
        {"Rol", Rol.class},
        {"Usuario", Usuario.class},
        {"Mesa", Mesa.class},
        {"Categoría", Categoria.class},
        {"Producto", Producto.class},
        {"Pedido", Pedido.class},
        {"Detalle de pedido", DetallePedido.class},
        {"Historial de estado", HistorialEstado.class},
        {"Factura", Factura.class},
    };

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int opcion;

        do {
            System.out.println("========= RESTAURANTE - CRUD CON ORM (Hibernate) =========");
            for (int i = 0; i < TABLAS.length; i++) {
                System.out.println((i + 1) + ". " + TABLAS[i][0]);
            }
            System.out.println("0. Salir");
            System.out.print("Elige una tabla: ");

            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                opcion = -1;
            }

            if (opcion >= 1 && opcion <= TABLAS.length) {
                abrir((String) TABLAS[opcion - 1][0], (Class<?>) TABLAS[opcion - 1][1], sc);
            } else if (opcion != 0) {
                System.out.println("Opción no válida.");
            }
            System.out.println();
        } while (opcion != 0);

        JPAUtil.cerrar();
        sc.close();
        System.out.println("Hasta pronto.");
    }

    private static <T> void abrir(String titulo, Class<T> clase, Scanner sc) {
        new ConsolaCrud<>(titulo, clase, sc).ejecutar();
    }
}
