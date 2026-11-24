package api.exception;

import jakarta.persistence.PersistenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** Convierte las excepciones del servicio en respuestas HTTP con un JSON de error. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> invalida(OperacionInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ProhibidoException.class)
    public ResponseEntity<Map<String, Object>> prohibido(ProhibidoException e) {
        return responder(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** RF-01: sin sesión o con token vencido. */
    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<Map<String, Object>> noAutenticado(NoAutenticadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene valores con "
                + "formato incorrecto (las fechas van como 2026-10-12T19:00:00).");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El valor '" + e.getValue() + "' no es válido para '" + e.getName() + "'.");
    }

    /** Errores de la base de datos (valor duplicado, llave foránea inexistente, campo obligatorio...). */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, Object>> baseDeDatos(PersistenceException e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return responder(HttpStatus.CONFLICT, "No se pudo guardar en la base de datos: " + causa.getMessage());
    }

    private ResponseEntity<Map<String, Object>> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("status", estado.value(), "error", mensaje));
    }
}
