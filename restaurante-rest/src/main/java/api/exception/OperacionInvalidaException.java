package api.exception;

/** Datos inválidos o llaves foráneas que no existen (se responde 400). */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
