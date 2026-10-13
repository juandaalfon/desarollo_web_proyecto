package dao;

import conexion.ConexionBD;
import model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de UsuarioDAO usando JDBC puro.
 * Cada método abre su propia conexión y la cierra con try-with-resources.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String INSERT =
            "INSERT INTO usuario (nombre, apellido, cedula, correo, telefono, contrasena) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM usuario WHERE id_usuario = ?";

    private static final String SELECT_ALL =
            "SELECT * FROM usuario";

    private static final String UPDATE =
            "UPDATE usuario SET nombre = ?, apellido = ?, cedula = ?, correo = ?, telefono = ?, contrasena = ? " +
            "WHERE id_usuario = ?";

    private static final String DELETE =
            "DELETE FROM usuario WHERE id_usuario = ?";

    private static final String SELECT_BY_CORREO =
            "SELECT * FROM usuario WHERE correo = ?";

    @Override
    public boolean crear(Usuario usuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setInt(3, usuario.getCedula());
            ps.setString(4, usuario.getCorreo());
            ps.setLong(5, usuario.getTelefono());
            ps.setString(6, usuario.getContrasena());

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                return false;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setIdUsuario(rs.getInt(1));
                }
            }
            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Usuario obtenerPorId(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener usuario por id: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Usuario> obtenerTodos() {
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return usuarios;
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setInt(3, usuario.getCedula());
            ps.setString(4, usuario.getCorreo());
            ps.setLong(5, usuario.getTelefono());
            ps.setString(6, usuario.getContrasena());
            ps.setInt(7, usuario.getIdUsuario());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminar(int idUsuario) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(DELETE)) {

            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Usuario obtenerPorCorreo(String correo) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_CORREO)) {

            ps.setString(1, correo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar usuario por correo: " + e.getMessage());
        }
        return null;
    }

    /** Convierte la fila actual del ResultSet en un objeto Usuario. */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellido(rs.getString("apellido"));
        usuario.setCedula(rs.getInt("cedula"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setTelefono(rs.getLong("telefono"));
        usuario.setContrasena(rs.getString("contrasena"));
        return usuario;
    }
}
