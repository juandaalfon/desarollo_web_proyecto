package api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import security.Permisos;
import security.Sesion;
import service.AuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RF-01 en la API: antes de ejecutar cualquier ruta /api/** exige un token válido
 * (header  Authorization: Bearer <token>)  y comprueba que el rol del usuario pueda usar esa ruta.
 *
 *   sin token o token vencido  -> 401
 *   rol sin permiso            -> 403
 *
 * La única ruta pública es POST /api/auth/login.
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "sesionActual";

    private final AuthService auth = new AuthService();

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object handler)
            throws IOException {
        // Las peticiones OPTIONS (CORS) las contesta el navegador/servidor sin credenciales
        if ("OPTIONS".equalsIgnoreCase(peticion.getMethod())) {
            return true;
        }
        String ruta = peticion.getRequestURI();
        if (ruta.equals("/api/auth/login")) {
            return true;
        }

        Sesion sesion = auth.validar(extraerToken(peticion));
        if (sesion == null) {
            responder(respuesta, 401, "Debes iniciar sesión: haz POST /api/auth/login y envía el header "
                    + "Authorization: Bearer <token>.");
            return false;
        }
        if (!Permisos.permitido(sesion.tipoRol(), peticion.getMethod(), ruta)) {
            responder(respuesta, 403, "Tu rol (" + sesion.rol() + ") no tiene permiso para "
                    + peticion.getMethod() + " " + ruta + ".");
            return false;
        }

        peticion.setAttribute(ATRIBUTO_SESION, sesion);
        return true;
    }

    public static String extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera != null && cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return cabecera.substring(7).trim();
        }
        return null;
    }

    private void responder(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String seguro = mensaje.replace("\\", "\\\\").replace("\"", "\\\"");
        respuesta.getWriter().write("{\"status\":" + estado + ",\"error\":\"" + seguro + "\"}");
    }
}
