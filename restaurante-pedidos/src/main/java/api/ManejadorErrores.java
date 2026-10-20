package api;

import jakarta.persistence.PersistenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import service.ServicioException;

import java.util.Map;

/** Convierte las excepciones en respuestas JSON  {"status": 404, "error": "mensaje"}. */
@RestControllerAdvice
public class ManejadorErrores {

    /** Reglas de negocio y errores de validación de los servicios (400, 401, 403, 404, 409). */
    @ExceptionHandler(ServicioException.class)
    public ResponseEntity<Map<String, Object>> servicio(ServicioException e) {
        return cuerpo(e.getEstado(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return cuerpo(400, "El cuerpo de la petición no es un JSON válido o tiene valores con formato incorrecto "
                + "(las fechas van como 2026-10-12T19:00:00).");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return cuerpo(400, "El valor '" + e.getValue() + "' no es válido para '" + e.getName() + "'.");
    }

    /** Errores de la base de datos (valor duplicado, llave foránea inexistente, campo obligatorio...). */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, Object>> baseDeDatos(PersistenceException e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return cuerpo(409, "No se pudo guardar en la base de datos: " + causa.getMessage());
    }

    private ResponseEntity<Map<String, Object>> cuerpo(int estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("status", estado, "error", mensaje));
    }
}
