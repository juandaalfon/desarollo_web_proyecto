package service;

import model.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import security.Sesion;
import security.TipoRol;
import util.JPAUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RF-01: autenticación. Valida usuario y contraseña (BCrypt) y entrega un token de sesión.
 * Las sesiones viven en memoria: si se reinicia el servidor hay que volver a iniciar sesión.
 */
public class AuthService {

    private static final long HORAS_DE_SESION = 8;
    private static final Map<String, Sesion> SESIONES = new ConcurrentHashMap<>();
    private static final BCryptPasswordEncoder CIFRADOR = new BCryptPasswordEncoder();

    public Sesion login(String usuarioLogin, String contrasena) {
        if (usuarioLogin == null || usuarioLogin.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw ServicioException.invalida("El usuario y la contraseña son obligatorios.");
        }

        Usuario u = JPAUtil.enLectura(em -> {
            List<Usuario> encontrados = em
                    .createQuery("SELECT u FROM Usuario u WHERE u.usuarioLogin = ?1", Usuario.class)
                    .setParameter(1, usuarioLogin.trim())
                    .setMaxResults(1)
                    .getResultList();
            return encontrados.isEmpty() ? null : encontrados.get(0);
        });

        // Mismo mensaje para "no existe" y "clave incorrecta": no se revela cuál falló
        if (u == null || !coincide(contrasena, u.getContrasenaHash())) {
            throw ServicioException.noAutenticado("Usuario o contraseña incorrectos.");
        }
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw ServicioException.noAutenticado("El usuario está inactivo.");
        }
        TipoRol tipo = TipoRol.deNombre(u.getRol().getNombreRol());
        if (tipo == null) {
            throw ServicioException.prohibido("El rol '" + u.getRol().getNombreRol()
                    + "' no tiene acceso al sistema (usa Administrador, Mesero o Cocina).");
        }

        String token = UUID.randomUUID().toString();
        Sesion sesion = new Sesion(token, u.getIdUsuario(), u.getUsuarioLogin(), u.getNombre(),
                u.getRol().getNombreRol(), tipo, LocalDateTime.now().plusHours(HORAS_DE_SESION));
        SESIONES.put(token, sesion);
        return sesion;
    }

    /** Devuelve la sesión del token, o null si no existe o ya venció. */
    public Sesion validar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Sesion s = SESIONES.get(token);
        if (s == null) {
            return null;
        }
        if (s.expira().isBefore(LocalDateTime.now())) {
            SESIONES.remove(token);
            return null;
        }
        return s;
    }

    public void logout(String token) {
        if (token != null) {
            SESIONES.remove(token);
        }
    }

    private boolean coincide(String contrasena, String hash) {
        try {
            return hash != null && CIFRADOR.matches(contrasena, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
