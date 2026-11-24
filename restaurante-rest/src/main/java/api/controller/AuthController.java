package api.controller;

import api.security.AuthInterceptor;
import api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.Sesion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RF-01: autenticación de usuarios.
 *
 *   POST /api/auth/login   {"usuarioLogin": "mesero", "contrasena": "mesero123"}  -> token (público)
 *   GET  /api/auth/me      quién soy y con qué rol
 *   POST /api/auth/logout  cierra la sesión (invalida el token)
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credenciales) {
        Sesion sesion = auth.login(credenciales.get("usuarioLogin"), credenciales.get("contrasena"));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", sesion.token());
        respuesta.put("tipo", "Bearer");
        respuesta.put("expira", sesion.expira());
        respuesta.putAll(datosDeUsuario(sesion));
        return respuesta;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest peticion) {
        return datosDeUsuario(Sesion.de(peticion));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest peticion) {
        auth.logout(AuthInterceptor.extraerToken(peticion));
        return Map.of("mensaje", "Sesión cerrada.");
    }

    private Map<String, Object> datosDeUsuario(Sesion sesion) {
        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("idUsuario", sesion.idUsuario());
        usuario.put("nombre", sesion.nombre());
        usuario.put("usuarioLogin", sesion.usuarioLogin());
        usuario.put("rol", sesion.rol());
        return Map.of("usuario", usuario);
    }
}
