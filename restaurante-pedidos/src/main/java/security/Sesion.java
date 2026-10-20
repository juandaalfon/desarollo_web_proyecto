package security;

import service.ServicioException;

import java.time.LocalDateTime;

/** Sesión iniciada: quién es el usuario, su rol y hasta cuándo vale el token. */
public record Sesion(String token, Integer idUsuario, String usuarioLogin, String nombre,
                     String rol, TipoRol tipoRol, LocalDateTime expira) {

    /** Lanza 403 si el rol de la sesión no está entre los permitidos. */
    public void exigirRol(TipoRol... permitidos) {
        for (TipoRol p : permitidos) {
            if (p == tipoRol) {
                return;
            }
        }
        throw ServicioException.prohibido("El rol " + rol + " no puede realizar esta acción.");
    }
}
