package model;

/**
 * Modelo que representa un registro de la tabla "producto".
 */
public class Producto {

    private int idProducto;
    private String nombre;
    private String descripcion;
    private double precio;
    private boolean disponible;
    private int menuIdMenu;

    public Producto() {
    }

    /** Constructor completo, útil al leer datos desde la base de datos. */
    public Producto(int idProducto, String nombre, String descripcion,
                    double precio, boolean disponible, int menuIdMenu) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
        this.menuIdMenu = menuIdMenu;
    }

    /** Constructor sin id, útil al crear un producto nuevo. */
    public Producto(String nombre, String descripcion,
                    double precio, boolean disponible, int menuIdMenu) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
        this.menuIdMenu = menuIdMenu;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public int getMenuIdMenu() {
        return menuIdMenu;
    }

    public void setMenuIdMenu(int menuIdMenu) {
        this.menuIdMenu = menuIdMenu;
    }

    @Override
    public String toString() {
        return "Producto{" +
                "idProducto=" + idProducto +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                ", disponible=" + disponible +
                ", menuIdMenu=" + menuIdMenu +
                '}';
    }
}