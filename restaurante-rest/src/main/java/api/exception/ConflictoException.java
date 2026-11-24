package api.exception;

/** El registro no se puede eliminar porque otra tabla depende de él (se responde 409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
