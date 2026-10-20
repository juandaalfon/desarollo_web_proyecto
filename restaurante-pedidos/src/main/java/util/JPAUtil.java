package util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Crea una única EntityManagerFactory (costosa) y entrega EntityManager (baratos) bajo demanda.
 * Equivale a la clase ConexionBD de los CRUD con JDBC, pero la conexión la gestiona Hibernate.
 *
 * Además ofrece dos ayudas para los servicios, que necesitan varias operaciones en una sola unidad de trabajo:
 *  - enLectura:     abre un EntityManager, ejecuta una consulta y lo cierra.
 *  - enTransaccion: igual, pero dentro de una transacción (commit si todo sale bien, rollback si hay error).
 */
public final class JPAUtil {

    private static EntityManagerFactory emf;

    private JPAUtil() {
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Map<String, String> sobreescribir = new HashMap<>();
            poner(sobreescribir, "DB_URL", "jakarta.persistence.jdbc.url");
            poner(sobreescribir, "DB_USER", "jakarta.persistence.jdbc.user");
            poner(sobreescribir, "DB_PASSWORD", "jakarta.persistence.jdbc.password");
            emf = Persistence.createEntityManagerFactory("restaurantePU", sobreescribir);
        }
        return emf;
    }

    public static EntityManager crearEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /** Ejecuta una consulta con un EntityManager que se cierra al terminar. */
    public static <R> R enLectura(Function<EntityManager, R> trabajo) {
        EntityManager em = crearEntityManager();
        try {
            return trabajo.apply(em);
        } finally {
            em.close();
        }
    }

    /**
     * Ejecuta el trabajo dentro de una transacción. Si el trabajo lanza cualquier excepción
     * (por ejemplo una ServicioException de regla de negocio) se hace rollback y se vuelve a lanzar.
     */
    public static <R> R enTransaccion(Function<EntityManager, R> trabajo) {
        EntityManager em = crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R resultado = trabajo.apply(em);
            tx.commit();
            return resultado;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static synchronized void cerrar() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private static void poner(Map<String, String> mapa, String variableEntorno, String propiedad) {
        String valor = System.getenv(variableEntorno);
        if (valor != null) {
            mapa.put(propiedad, valor);
        }
    }
}
