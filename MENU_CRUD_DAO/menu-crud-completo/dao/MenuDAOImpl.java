package dao;

import conexion.ConexionBD;
import model.Menu;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de MenuDAO usando JDBC puro.
 * Cada método abre su propia conexión y la cierra con try-with-resources.
 */
public class MenuDAOImpl implements MenuDAO {

    private static final String INSERT =
            "INSERT INTO menu (nombre) VALUES (?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM menu WHERE idmenu = ?";

    private static final String SELECT_ALL =
            "SELECT * FROM menu";

    private static final String UPDATE =
            "UPDATE menu SET nombre = ? WHERE idmenu = ?";

    private static final String DELETE =
            "DELETE FROM menu WHERE idmenu = ?";

    @Override
    public boolean crear(Menu menu) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, menu.getNombre());

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas == 0) {
                return false;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    menu.setIdMenu(rs.getInt(1));
                }
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear menú: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Menu obtenerPorId(int idMenu) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, idMenu);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMenu(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener menú por id: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Menu> obtenerTodos() {

        List<Menu> menus = new ArrayList<>();

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                menus.add(mapearMenu(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar menús: " + e.getMessage());
        }

        return menus;
    }

    @Override
    public boolean actualizar(Menu menu) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, menu.getNombre());
            ps.setInt(2, menu.getIdMenu());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar menú: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminar(int idMenu) {

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(DELETE)) {

            ps.setInt(1, idMenu);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar menú: " + e.getMessage());
            return false;
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Menu. */
    private Menu mapearMenu(ResultSet rs) throws SQLException {

        Menu menu = new Menu();

        menu.setIdMenu(rs.getInt("idmenu"));
        menu.setNombre(rs.getString("nombre"));

        return menu;
    }
}