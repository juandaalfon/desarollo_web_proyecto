package model;

/**
 * Modelo que representa un registro de la tabla "usuario".
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private int cedula;
    private String correo;
    private long telefono; // long en vez de int: un celular de 10 dígitos no cabe en INT
    private String contrasena;

    public Usuario() {
    }

    /** Constructor completo, útil al leer datos desde la base de datos. */
    public Usuario(int idUsuario, String nombre, String apellido, int cedula,
                    String correo, long telefono, String contrasena) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.cedula = cedula;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasena = contrasena;
    }

    /** Constructor sin id, útil al crear un usuario nuevo (el id lo genera la BD). */
    public Usuario(String nombre, String apellido, int cedula,
                    String correo, long telefono, String contrasena) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.cedula = cedula;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasena = contrasena;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getCedula() {
        return cedula;
    }

    public void setCedula(int cedula) {
        this.cedula = cedula;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public long getTelefono() {
        return telefono;
    }

    public void setTelefono(long telefono) {
        this.telefono = telefono;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    @Override
    public String toString() {
        // Se omite la contraseña intencionalmente al imprimir el objeto.
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", cedula=" + cedula +
                ", correo='" + correo + '\'' +
                ", telefono=" + telefono +
                '}';
    }
}
