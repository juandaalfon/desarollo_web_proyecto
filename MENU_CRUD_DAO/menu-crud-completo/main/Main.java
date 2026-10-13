package main;

import dao.MenuDAO;
import dao.MenuDAOImpl;
import model.Menu;

import java.util.List;

/**
 * Clase de prueba que demuestra las 4 operaciones CRUD sobre "menu".
 */
public class Main {

    public static void main(String[] args) {

        MenuDAO menuDAO = new MenuDAOImpl();

        // ---------- CREATE ----------

        Menu nuevo = new Menu("Menú Ejecutivo");

        boolean creado = menuDAO.crear(nuevo);

        System.out.println("¿Menú creado? " + creado +
                " -> id generado: " + nuevo.getIdMenu());

        // ---------- READ (por id) ----------

        Menu encontrado =
                menuDAO.obtenerPorId(nuevo.getIdMenu());

        System.out.println("Menú encontrado: " + encontrado);

        // ---------- READ (todos) ----------

        List<Menu> todos =
                menuDAO.obtenerTodos();

        System.out.println("Total de menús: " + todos.size());

        todos.forEach(System.out::println);

        // ---------- UPDATE ----------

        if (encontrado != null) {

            encontrado.setNombre("Menú Premium");

            boolean actualizado =
                    menuDAO.actualizar(encontrado);

            System.out.println("¿Menú actualizado? " + actualizado);
        }

        // ---------- DELETE ----------

        boolean eliminado =
                menuDAO.eliminar(nuevo.getIdMenu());

        System.out.println("¿Menú eliminado? " + eliminado);
    }
}