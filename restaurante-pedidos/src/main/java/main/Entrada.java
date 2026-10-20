package main;

import java.util.Scanner;

/** Lectura de datos por teclado con validación, compartida por todos los menús de la consola. */
public class Entrada {

    private final Scanner sc;

    public Entrada(Scanner sc) {
        this.sc = sc;
    }

    public Scanner scanner() {
        return sc;
    }

    /** Lee una línea de texto (puede quedar vacía). */
    public String texto(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine().trim();
    }

    /** Pide un número entero hasta que se escriba uno válido. */
    public int entero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor escribe un número entero válido.");
            }
        }
    }

    /** Igual que entero(), pero si se pulsa Enter devuelve null. */
    public Integer enteroOpcional(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = sc.nextLine().trim();
            if (linea.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(linea);
            } catch (NumberFormatException e) {
                System.out.println("Por favor escribe un número entero válido (o Enter para omitir).");
            }
        }
    }
}
