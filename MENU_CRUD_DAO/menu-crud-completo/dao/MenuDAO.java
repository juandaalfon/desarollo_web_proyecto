package dao;

import model.Menu;

import java.util.List;

/**
 * Contrato DAO para las operaciones CRUD sobre la tabla "menu".
 */
public interface MenuDAO {

    /** Inserta un menú nuevo. Devuelve true si la operación fue exitosa. */
    boolean crear(Menu menu);

    /** Busca un menú por su id. Devuelve null si no existe. */
    Menu obtenerPorId(int idMenu);

    /** Devuelve todos los menús registrados. */
    List<Menu> obtenerTodos();

    /** Actualiza los datos de un menú existente. */
    boolean actualizar(Menu menu);

    /** Elimina un menú por su id. */
    boolean eliminar(int idMenu);
}