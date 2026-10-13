package model;

/**
 * Modelo que representa un registro de la tabla "menu".
 */
public class Menu {

    private int idMenu;
    private String nombre;

    public Menu() {
    }

    /** Constructor completo, útil al leer datos desde la base de datos. */
    public Menu(int idMenu, String nombre) {
        this.idMenu = idMenu;
        this.nombre = nombre;
    }

    /** Constructor sin id, útil al crear un menú nuevo. */
    public Menu(String nombre) {
        this.nombre = nombre;
    }

    public int getIdMenu() {
        return idMenu;
    }

    public void setIdMenu(int idMenu) {
        this.idMenu = idMenu;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "Menu{" +
                "idMenu=" + idMenu +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}