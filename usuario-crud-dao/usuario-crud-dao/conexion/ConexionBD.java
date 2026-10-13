package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria encargada de abrir y cerrar la conexión JDBC.
 * Ajusta URL, USUARIO y PASSWORD según tu entorno.
 */
public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/crud_usuario";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";

    private ConexionBD() {
        // Clase utilitaria: no se debe instanciar.
    }

    /** Abre y devuelve una nueva conexión a la base de datos. */
    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver JDBC de MySQL en el classpath", e);
        }
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    /** Cierra una conexión de forma segura. */
    public static void cerrarConexion(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }
}
