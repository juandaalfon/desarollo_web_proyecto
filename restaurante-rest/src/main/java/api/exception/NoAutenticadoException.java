package api.exception;

/** No hay sesión iniciada o el token no es válido / venció (se responde 401). */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
