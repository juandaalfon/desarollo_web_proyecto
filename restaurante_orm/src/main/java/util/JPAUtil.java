package util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Crea una única EntityManagerFactory (costosa) y entrega EntityManager (baratos) bajo demanda.
 * Equivale a la clase ConexionBD de los CRUD con JDBC, pero la conexión la gestiona Hibernate.
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
