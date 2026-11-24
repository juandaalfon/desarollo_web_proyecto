package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import util.JPAUtil;

import java.util.Collections;
import java.util.List;

/**
 * DAO genérico: el mismo CRUD sirve para las 12 entidades porque el ORM (Hibernate)
 * genera el SQL. Cada operación abre su EntityManager y lo cierra al terminar.
 *
 * @param <T> clase de la entidad (cuya llave primaria es un Integer)
 */
public class GenericDAO<T> {

    private final Class<T> clase;

    public GenericDAO(Class<T> clase) {
        this.clase = clase;
    }

    /**
     * Fábrica: entrega el DAO adecuado para cada entidad. Usuario usa UsuarioDAO, que cifra
     * la contraseña con BCrypt antes de guardarla; las demás usan el DAO genérico.
     */
    @SuppressWarnings("unchecked")
    public static <E> GenericDAO<E> para(Class<E> clase) {
        if (clase == model.Usuario.class) {
            return (GenericDAO<E>) new UsuarioDAO();
        }
        return new GenericDAO<>(clase);
    }

    /** INSERT. Tras persistir, la entidad queda con su id generado. */
    public boolean crear(T entidad) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entidad);
            tx.commit();
            return true;
        } catch (RuntimeException e) {
            revertir(tx);
            System.err.println("Error al crear " + clase.getSimpleName() + ": " + causaRaiz(e));
            return false;
        } finally {
            em.close();
        }
    }

    /** SELECT por llave primaria. Devuelve null si no existe. */
    public T buscarPorId(Integer id) {
        return buscarEntidad(clase, id);
    }

    /** SELECT * */
    public List<T> listar() {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + clase.getSimpleName() + " e", clase).getResultList();
        } catch (RuntimeException e) {
            System.err.println("Error al listar " + clase.getSimpleName() + ": " + causaRaiz(e));
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    /** UPDATE (merge de una entidad con id existente). */
    public boolean actualizar(T entidad) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(entidad);
            tx.commit();
            return true;
        } catch (RuntimeException e) {
            revertir(tx);
            System.err.println("Error al actualizar " + clase.getSimpleName() + ": " + causaRaiz(e));
            return false;
        } finally {
            em.close();
        }
    }

    /** DELETE por llave primaria. Devuelve false si no existe o si otra tabla la referencia. */
    public boolean eliminar(Integer id) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T entidad = em.find(clase, id);
            if (entidad == null) {
                tx.rollback();
                return false;
            }
            em.remove(entidad);
            tx.commit();
            return true;
        } catch (RuntimeException e) {
            revertir(tx);
            System.err.println("Error al eliminar " + clase.getSimpleName() + ": " + causaRaiz(e));
            return false;
        } finally {
            em.close();
        }
    }

    /** Busca cualquier entidad por id (se usa para resolver las llaves foráneas). */
    public <R> R buscarEntidad(Class<R> tipo, Integer id) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return em.find(tipo, id);
        } catch (RuntimeException e) {
            System.err.println("Error al buscar " + tipo.getSimpleName() + ": " + causaRaiz(e));
            return null;
        } finally {
            em.close();
        }
    }

    private void revertir(EntityTransaction tx) {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
    }

    private String causaRaiz(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
