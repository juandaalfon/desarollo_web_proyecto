package main;

import dao.ProductoDAO;
import dao.ProductoDAOImpl;
import model.Producto;

import java.util.List;

/**
 * Clase de prueba que demuestra las 4 operaciones CRUD sobre "producto".
 */
public class Main {

    public static void main(String[] args) {

        ProductoDAO productoDAO = new ProductoDAOImpl();

        // ---------- CREATE ----------

        Producto nuevo = new Producto(
                "Hamburguesa Especial",
                "Hamburguesa con queso y tocineta",
                25000,
                true,
                1
        );

        boolean creado = productoDAO.crear(nuevo);

        System.out.println("¿Producto creado? " + creado +
                " -> id generado: " + nuevo.getIdProducto());

        // ---------- READ (por id) ----------

        Producto encontrado =
                productoDAO.obtenerPorId(nuevo.getIdProducto());

        System.out.println("Producto encontrado: " + encontrado);

        // ---------- READ (todos) ----------

        List<Producto> todos =
                productoDAO.obtenerTodos();

        System.out.println("Total de productos: " + todos.size());

        todos.forEach(System.out::println);

        // ---------- UPDATE ----------

        if (encontrado != null) {

            encontrado.setPrecio(28000);

            boolean actualizado =
                    productoDAO.actualizar(encontrado);

            System.out.println("¿Producto actualizado? " + actualizado);
        }

        // ---------- DELETE ----------

        boolean eliminado =
                productoDAO.eliminar(nuevo.getIdProducto());

        System.out.println("¿Producto eliminado? " + eliminado);
    }
}