package api;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import util.JPAUtil;
import util.Semilla;

/**
 * Punto de entrada de la API REST. Spring solo levanta el servidor web;
 * la base de datos se sigue manejando con JPAUtil + GenericDAO (Hibernate).
 */
@SpringBootApplication
public class RestauranteApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestauranteApplication.class, args);
    }

    /** Al arrancar crea los roles, los usuarios de prueba y el catálogo inicial si todavía no existen. */
    @Bean
    public ApplicationRunner cargarDatosIniciales() {
        return args -> Semilla.ejecutar();
    }

    /** Cierra la EntityManagerFactory cuando se detiene el servidor. */
    @PreDestroy
    public void cerrarBD() {
        JPAUtil.cerrar();
    }
}
