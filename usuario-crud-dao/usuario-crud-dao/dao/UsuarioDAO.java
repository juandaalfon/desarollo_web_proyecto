package dao;

import model.Usuario;

import java.util.List;

/**
 * Contrato DAO para las operaciones CRUD sobre la tabla "usuario".
 */
public interface UsuarioDAO {

    /** Inserta un usuario nuevo. Devuelve true si la operación fue exitosa. */
    boolean crear(Usuario usuario);

    /** Busca un usuario por su id. Devuelve null si no existe. */
    Usuario obtenerPorId(int idUsuario);

    /** Devuelve todos los usuarios registrados. */
    List<Usuario> obtenerTodos();

    /** Actualiza los datos de un usuario existente. */
    boolean actualizar(Usuario usuario);

    /** Elimina un usuario por su id. */
    boolean eliminar(int idUsuario);

    /** Busca un usuario por correo (útil para validar login o duplicados). */
    Usuario obtenerPorCorreo(String correo);
}
