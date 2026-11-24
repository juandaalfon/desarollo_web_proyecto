package security;

import api.service.TipoRol;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/** Sesión iniciada (RF-01): quién es el usuario, su rol y hasta cuándo vale el token. */
public record Sesion(String token, Integer idUsuario, String usuarioLogin, String nombre,
                     String rol, TipoRol tipoRol, LocalDateTime expira) {

    /** Nombre del atributo donde el AuthInterceptor deja la sesión de la petición en curso. */
    public static final String ATRIBUTO = "sesionActual";

    /** La sesión que dejó el AuthInterceptor al validar el token (null si la petición no la tiene). */
    public static Sesion de(HttpServletRequest peticion) {
        Object s = peticion.getAttribute(ATRIBUTO);
        return s instanceof Sesion ? (Sesion) s : null;
    }
}
