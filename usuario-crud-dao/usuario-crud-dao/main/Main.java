package main;

import dao.UsuarioDAO;
import dao.UsuarioDAOImpl;
import model.Usuario;

import java.util.List;
import java.util.Scanner;

/**
 * Menú interactivo por teclado para el CRUD de "usuario".
 * Permite crear, buscar, listar, actualizar y eliminar escribiendo
 * los datos directamente en la consola.
 */
public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    public static void main(String[] args) {
        int opcion;

        do {
            mostrarMenu();
            opcion = leerEntero("Elige una opción: ");

            switch (opcion) {
                case 1:
                    crear();
                    break;
                case 2:
                    buscarPorId();
                    break;
                case 3:
                    listarTodos();
                    break;
                case 4:
                    actualizar();
                    break;
                case 5:
                    eliminar();
                    break;
                case 6:
                    buscarPorCorreo();
                    break;
                case 0:
                    System.out.println("Saliendo del programa...");
                    break;
                default:
                    System.out.println("Opción no válida. Intenta de nuevo.");
                    break;
            }

            System.out.println();

        } while (opcion != 0);

        sc.close();
    }

    private static void mostrarMenu() {
        System.out.println("========= USUARIO (CRUD) =========");
        System.out.println("1. Crear usuario");
        System.out.println("2. Buscar usuario por id");
        System.out.println("3. Listar todos los usuarios");
        System.out.println("4. Actualizar usuario");
        System.out.println("5. Eliminar usuario");
        System.out.println("6. Buscar usuario por correo");
        System.out.println("0. Salir");
        System.out.println("===================================");
    }

    private static void crear() {
        System.out.println("--- Crear nuevo usuario ---");
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        int cedula = leerEntero("Cédula: ");
        String correo = leerTexto("Correo: ");
        long telefono = leerLong("Teléfono: ");
        String contrasena = leerTexto("Contraseña: ");

        Usuario nuevo = new Usuario(nombre, apellido, cedula, correo, telefono, contrasena);
        boolean creado = usuarioDAO.crear(nuevo);

        if (creado) {
            System.out.println("Usuario creado con éxito. Id generado: " + nuevo.getIdUsuario());
        } else {
            System.out.println("No se pudo crear el usuario (revisa si el correo ya existe).");
        }
    }

    private static void buscarPorId() {
        System.out.println("--- Buscar usuario por id ---");
        int id = leerEntero("Id del usuario a buscar: ");

        Usuario encontrado = usuarioDAO.obtenerPorId(id);

        if (encontrado != null) {
            System.out.println("Encontrado: " + encontrado);
        } else {
            System.out.println("No existe un usuario con ese id.");
        }
    }

    private static void listarTodos() {
        System.out.println("--- Listado de usuarios ---");
        List<Usuario> todos = usuarioDAO.obtenerTodos();

        if (todos.isEmpty()) {
            System.out.println("No hay usuarios registrados.");
        } else {
            todos.forEach(System.out::println);
        }
    }

    private static void actualizar() {
        System.out.println("--- Actualizar usuario ---");
        int id = leerEntero("Id del usuario a actualizar: ");

        Usuario existente = usuarioDAO.obtenerPorId(id);
        if (existente == null) {
            System.out.println("No existe un usuario con ese id.");
            return;
        }

        System.out.println("Datos actuales: " + existente);

        String nombre = leerTexto("Nuevo nombre: ");
        String apellido = leerTexto("Nuevo apellido: ");
        int cedula = leerEntero("Nueva cédula: ");
        String correo = leerTexto("Nuevo correo: ");
        long telefono = leerLong("Nuevo teléfono: ");
        String contrasena = leerTexto("Nueva contraseña: ");

        existente.setNombre(nombre);
        existente.setApellido(apellido);
        existente.setCedula(cedula);
        existente.setCorreo(correo);
        existente.setTelefono(telefono);
        existente.setContrasena(contrasena);

        boolean actualizado = usuarioDAO.actualizar(existente);
        System.out.println(actualizado ? "Usuario actualizado con éxito." : "No se pudo actualizar el usuario.");
    }

    private static void eliminar() {
        System.out.println("--- Eliminar usuario ---");
        int id = leerEntero("Id del usuario a eliminar: ");

        boolean eliminado = usuarioDAO.eliminar(id);
        System.out.println(eliminado ? "Usuario eliminado con éxito." : "No se pudo eliminar (verifica que el id exista).");
    }

    private static void buscarPorCorreo() {
        System.out.println("--- Buscar usuario por correo ---");
        String correo = leerTexto("Correo a buscar: ");

        Usuario encontrado = usuarioDAO.obtenerPorCorreo(correo);

        if (encontrado != null) {
            System.out.println("Encontrado: " + encontrado);
        } else {
            System.out.println("No existe un usuario con ese correo.");
        }
    }

    // Utilidades de lectura por teclado

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine().trim();
    }

    private static int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = sc.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Por favor escribe un número entero válido.");
            }
        }
    }

    private static long leerLong(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = sc.nextLine().trim();
            try {
                return Long.parseLong(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Por favor escribe un número válido (ej: 3001234567).");
            }
        }
    }
}