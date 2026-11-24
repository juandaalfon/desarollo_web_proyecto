package api.service;

import api.exception.NoAutenticadoException;
import api.exception.ProhibidoException;
import api.exception.RecursoNoEncontradoException;
import dao.GenericDAO;
import model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import security.Sesion;

import java.util.Arrays;

/**
 * Identifica quién hace la acción y verifica su rol.
 * En la API el usuario sale de la sesión del token (RF-01: AuthInterceptor la deja en la petición y los
 * controladores pasan su id); en la consola lo da la sesión del login. Aquí se revisa que exista, esté activo
 * y tenga uno de los roles permitidos.
 */
@Service
public class AccesoService {

    private final GenericDAO<Usuario> usuarioDAO = new GenericDAO<>(Usuario.class);

    /**
     * Id del usuario que tiene la sesión de la petición HTTP en curso (null si no hay petición o sesión).
     * Lo usan los CRUD genéricos (POST/PUT/PATCH/DELETE) que no reciben el usuario como parámetro.
     */
    public Integer usuarioActualId() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes) {
            Object sesion = ((ServletRequestAttributes) atributos).getRequest().getAttribute(Sesion.ATRIBUTO);
            if (sesion instanceof Sesion) {
                return ((Sesion) sesion).idUsuario();
            }
        }
        return null;
    }

    /** Devuelve el usuario si existe y está activo. */
    public Usuario identificar(Integer idUsuario) {
        if (idUsuario == null) {
            throw new NoAutenticadoException("Debes iniciar sesión para realizar esta acción.");
        }
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("Usuario con id " + idUsuario + " no existe.");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ProhibidoException("El usuario '" + usuario.getNombre() + "' está inactivo.");
        }
        return usuario;
    }

    /** Igual que identificar, pero además exige que el rol del usuario esté entre los permitidos. */
    public Usuario exigirRol(Integer idUsuario, TipoRol... permitidos) {
        Usuario usuario = identificar(idUsuario);
        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombreRol() : null;
        TipoRol tipo = TipoRol.de(nombreRol);
        if (tipo == null || !Arrays.asList(permitidos).contains(tipo)) {
            throw new ProhibidoException("El rol '" + nombreRol + "' no puede realizar esta acción.");
        }
        return usuario;
    }
}
