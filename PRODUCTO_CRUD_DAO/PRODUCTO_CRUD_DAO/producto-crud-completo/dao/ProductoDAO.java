package dao;

import model.Producto;

import java.util.List;

/**
 * Contrato DAO para las operaciones CRUD sobre la tabla "producto".
 */
public interface ProductoDAO {

    /** Inserta un producto nuevo. */
    boolean crear(Producto producto);

    /** Busca un producto por su id. */
    Producto obtenerPorId(int idProducto);

    /** Devuelve todos los productos registrados. */
    List<Producto> obtenerTodos();

    /** Actualiza los datos de un producto existente. */
    boolean actualizar(Producto producto);

    /** Elimina un producto por su id. */
    boolean eliminar(int idProducto);
}