package dao;

import conexion.ConexionBD;
import model.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ProductoDAO usando JDBC puro.
 * Cada método abre su propia conexión y la cierra con try-with-resources.
 */
public class ProductoDAOImpl implements ProductoDAO {

    private static final String INSERT =
            "INSERT INTO producto (nombre, descripcion, precio, disponible, menu_idmenu) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM producto WHERE idproducto = ?";

    private static final String SELECT_ALL =
            "SELECT * FROM producto";

    private static final String UPDATE =
            "UPDATE producto SET nombre = ?, descripcion = ?, precio = ?, disponible = ?, menu_idmenu = ? " +
            "WHERE idproducto = ?";

    private static final String DELETE =
            "DELETE FROM producto WHERE idproducto = ?";

    @Override
    public boolean crear(Producto producto) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio());
            ps.setBoolean(4, producto.isDisponible());
            ps.setInt(5, producto.getMenuIdMenu());

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas == 0) {
                return false;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    producto.setIdProducto(rs.getInt(1));
                }
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear producto: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Producto obtenerPorId(int idProducto) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener producto por id: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Producto> obtenerTodos() {

        List<Producto> productos = new ArrayList<>();

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar productos: " + e.getMessage());
        }

        return productos;
    }

    @Override
    public boolean actualizar(Producto producto) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio());
            ps.setBoolean(4, producto.isDisponible());
            ps.setInt(5, producto.getMenuIdMenu());
            ps.setInt(6, producto.getIdProducto());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminar(int idProducto) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(DELETE)) {

            ps.setInt(1, idProducto);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar producto: " + e.getMessage());
            return false;
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Producto. */
    private Producto mapearProducto(ResultSet rs) throws SQLException {

        Producto producto = new Producto();

        producto.setIdProducto(rs.getInt("idproducto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getDouble("precio"));
        producto.setDisponible(rs.getBoolean("disponible"));
        producto.setMenuIdMenu(rs.getInt("menu_idmenu"));

        return producto;
    }
}