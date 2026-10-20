package main;

import security.Sesion;
import service.AuthService;
import service.ServicioException;
import util.JPAUtil;
import util.Semilla;

import java.util.Scanner;

/**
 * Consola con teclado. RF-01: primero se inicia sesión con usuario y contraseña y, según el rol,
 * se abre el menú de Administración, de Mesero o de Cocina.
 *
 * Usuarios de prueba (los crea la Semilla la primera vez):
 *   admin / admin123     mesero / mesero123     cocina / cocina123
 */
public class Main {

    private static final int MAX_INTENTOS = 3;

    public static void main(String[] args) {
        Entrada in = new Entrada(new Scanner(System.in));
        try {
            Semilla.ejecutar();
            Sesion sesion = iniciarSesion(in);
            if (sesion != null) {
                abrirMenu(sesion, in);
            }
        } finally {
            JPAUtil.cerrar();
        }
        System.out.println("Hasta pronto.");
    }

    private static Sesion iniciarSesion(Entrada in) {
        AuthService auth = new AuthService();
        System.out.println("RESTAURANTE - INICIAR SESION ");
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            String usuario = in.texto("Usuario: ");
            String contrasena = in.texto("Contraseña: ");
            try {
                Sesion sesion = auth.login(usuario, contrasena);
                System.out.println("Bienvenido, " + sesion.nombre() + " (" + sesion.rol() + ")");
                System.out.println();
                return sesion;
            } catch (ServicioException e) {
                System.out.println(e.getMessage() + " (intento " + intento + " de " + MAX_INTENTOS + ")");
            }
        }
        System.out.println("Demasiados intentos fallidos.");
        return null;
    }

    private static void abrirMenu(Sesion sesion, Entrada in) {
        switch (sesion.tipoRol()) {
            case ADMINISTRACION:
                new MenuAdmin(in).ejecutar();
                break;
            case MESERO:
                new MenuMesero(sesion, in).ejecutar();
                break;
            case COCINA:
                new MenuCocina(sesion, in).ejecutar();
                break;
            default:
                System.out.println("Tu rol no tiene un menú asignado.");
                break;
        }
    }
}
