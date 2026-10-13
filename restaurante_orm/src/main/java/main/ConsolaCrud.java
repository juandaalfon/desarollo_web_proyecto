package main;

import dao.GenericDAO;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de consola CRUD reutilizable para cualquier entidad. Lee los atributos de la clase
 * por reflexión (nombre, tipo y si es obligatorio) y pide cada dato por teclado, de modo que
 * no hace falta escribir un menú distinto por tabla.
 */
public class ConsolaCrud<T> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String titulo;
    private final Class<T> clase;
    private final GenericDAO<T> dao;
    private final Scanner sc;

    public ConsolaCrud(String titulo, Class<T> clase, Scanner sc) {
        this.titulo = titulo;
        this.clase = clase;
        this.dao = new GenericDAO<>(clase);
        this.sc = sc;
    }

    public void ejecutar() {
        int opcion;
        do {
            System.out.println("========= " + titulo.toUpperCase() + " (CRUD con ORM) =========");
            System.out.println("1. Crear");
            System.out.println("2. Buscar por id");
            System.out.println("3. Listar todos");
            System.out.println("4. Actualizar");
            System.out.println("5. Eliminar");
            System.out.println("0. Volver al menú principal");
            opcion = leerEntero("Elige una opción: ");

            switch (opcion) {
                case 1: crear(); break;
                case 2: buscarPorId(); break;
                case 3: listar(); break;
                case 4: actualizar(); break;
                case 5: eliminar(); break;
                case 0: break;
                default: System.out.println("Opción no válida."); break;
            }
            System.out.println();
        } while (opcion != 0);
    }

    // ------------------------------------------------------------------ operaciones

    private void crear() {
        try {
            T entidad = clase.getDeclaredConstructor().newInstance();
            for (Field campo : campos()) {
                if (!campo.isAnnotationPresent(Id.class)) {
                    pedirCampo(entidad, campo, false);
                }
            }
            if (dao.crear(entidad)) {
                System.out.println("Creado con éxito: " + entidad);
            } else {
                System.out.println("No se pudo crear (revisa los datos, llaves foráneas o valores únicos).");
            }
        } catch (ReflectiveOperationException e) {
            System.out.println("Error interno: " + e);
        }
    }

    private void buscarPorId() {
        T encontrado = dao.buscarPorId(leerEntero("Id a buscar: "));
        System.out.println(encontrado != null ? "Encontrado: " + encontrado : "No existe un registro con ese id.");
    }

    private void listar() {
        List<T> todos = dao.listar();
        if (todos.isEmpty()) {
            System.out.println("No hay registros.");
        } else {
            todos.forEach(System.out::println);
        }
    }

    private void actualizar() {
        T existente = dao.buscarPorId(leerEntero("Id a actualizar: "));
        if (existente == null) {
            System.out.println("No existe un registro con ese id.");
            return;
        }
        System.out.println("Datos actuales: " + existente);
        System.out.println("(Pulsa Enter para conservar el valor actual de un campo)");
        for (Field campo : campos()) {
            if (!campo.isAnnotationPresent(Id.class)) {
                pedirCampo(existente, campo, true);
            }
        }
        System.out.println(dao.actualizar(existente) ? "Actualizado con éxito." : "No se pudo actualizar.");
    }

    private void eliminar() {
        boolean ok = dao.eliminar(leerEntero("Id a eliminar: "));
        System.out.println(ok ? "Eliminado con éxito."
                : "No se pudo eliminar (¿el id no existe o hay registros de otras tablas que lo usan?).");
    }

    // ------------------------------------------------------------------ lectura por reflexión

    private Field[] camposArray;

    private Field[] campos() {
        if (camposArray == null) {
            camposArray = java.util.Arrays.stream(clase.getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .toArray(Field[]::new);
        }
        return camposArray;
    }

    private void pedirCampo(T entidad, Field campo, boolean actualizando) {
        campo.setAccessible(true);
        boolean requerido = esRequerido(campo);
        String etiqueta = campo.getName() + " (" + describirTipo(campo) + ")"
                + (requerido ? "" : " [opcional]") + ": ";

        while (true) {
            System.out.print(etiqueta);
            String entrada = sc.nextLine().trim();

            try {
                if (entrada.isEmpty()) {
                    if (actualizando) {
                        return; // conserva el valor actual
                    }
                    if (!requerido) {
                        campo.set(entidad, null);
                        return;
                    }
                    System.out.println("Este campo es obligatorio.");
                    continue;
                }
                campo.set(entidad, convertir(campo, entrada));
                return;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (RuntimeException e) {
                System.out.println("Valor no válido: " + e.getMessage());
            }
        }
    }

    private Object convertir(Field campo, String entrada) {
        Class<?> tipo = campo.getType();
        if (campo.isAnnotationPresent(ManyToOne.class)) {
            Object referencia = dao.buscarEntidad(tipo, Integer.parseInt(entrada));
            if (referencia == null) {
                throw new IllegalArgumentException("no existe " + tipo.getSimpleName() + " con id " + entrada);
            }
            return referencia;
        }
        if (tipo == String.class) return entrada;
        if (tipo == Boolean.class) {
            String v = entrada.toLowerCase();
            if (v.equals("s") || v.equals("si") || v.equals("sí") || v.equals("true")) return Boolean.TRUE;
            if (v.equals("n") || v.equals("no") || v.equals("false")) return Boolean.FALSE;
            throw new IllegalArgumentException("responde s (sí) o n (no)");
        }
        if (tipo == Integer.class) return Integer.valueOf(entrada);
        if (tipo == Long.class) return Long.valueOf(entrada);
        if (tipo == BigDecimal.class) return new BigDecimal(entrada);
        if (tipo == LocalDateTime.class) {
            try {
                return LocalDateTime.parse(entrada, FORMATO_FECHA);
            } catch (RuntimeException e) {
                return LocalDateTime.parse(entrada); // también acepta 2026-10-12T14:30:00
            }
        }
        throw new IllegalArgumentException("tipo no soportado: " + tipo.getSimpleName());
    }

    private boolean esRequerido(Field campo) {
        JoinColumn jc = campo.getAnnotation(JoinColumn.class);
        if (jc != null) {
            return !jc.nullable();
        }
        Column c = campo.getAnnotation(Column.class);
        return c != null && !c.nullable();
    }

    private String describirTipo(Field campo) {
        if (campo.isAnnotationPresent(ManyToOne.class)) {
            return "id de " + campo.getType().getSimpleName();
        }
        Class<?> t = campo.getType();
        if (t == LocalDateTime.class) return "fecha: aaaa-MM-dd HH:mm";
        if (t == BigDecimal.class) return "decimal, ej: 25000.50";
        if (t == String.class) return "texto";
        if (t == Boolean.class) return "s / n";
        return "número entero";
    }

    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor escribe un número entero válido.");
            }
        }
    }
}
