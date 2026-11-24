package api.exception;

/** El usuario no puede hacer esta acción por su rol o porque está inactivo (se responde 403). */
public class ProhibidoException extends RuntimeException {
    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
