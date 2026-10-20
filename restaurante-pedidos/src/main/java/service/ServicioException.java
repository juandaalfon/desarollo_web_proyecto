package service;

/**
 * Error de regla de negocio. Lleva el código HTTP que debe responder la API
 * (la consola solo muestra el mensaje).
 */
public class ServicioException extends RuntimeException {

    private final int estado;

    public ServicioException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int getEstado() {
        return estado;
    }

    public static ServicioException invalida(String mensaje) {
        return new ServicioException(400, mensaje);
    }

    public static ServicioException noAutenticado(String mensaje) {
        return new ServicioException(401, mensaje);
    }

    public static ServicioException prohibido(String mensaje) {
        return new ServicioException(403, mensaje);
    }

    public static ServicioException noEncontrado(String mensaje) {
        return new ServicioException(404, mensaje);
    }

    public static ServicioException conflicto(String mensaje) {
        return new ServicioException(409, mensaje);
    }
}
