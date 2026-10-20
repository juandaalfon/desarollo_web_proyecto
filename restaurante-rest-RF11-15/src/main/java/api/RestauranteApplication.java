package api;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import util.JPAUtil;

/**
 * Punto de entrada de la API REST. Spring solo levanta el servidor web;
 * la base de datos se sigue manejando con tu JPAUtil + GenericDAO (Hibernate).
 */
@SpringBootApplication
public class RestauranteApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestauranteApplication.class, args);
    }

    /** Cierra la EntityManagerFactory cuando se detiene el servidor. */
    @PreDestroy
    public void cerrarBD() {
        JPAUtil.cerrar();
    }
}
