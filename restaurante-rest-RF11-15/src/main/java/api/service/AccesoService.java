package api.service;

import api.exception.OperacionInvalidaException;
import api.exception.ProhibidoException;
import api.exception.RecursoNoEncontradoException;
import dao.GenericDAO;
import model.Usuario;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Identifica quién hace la petición y verifica su rol.
 * El proyecto aún no tiene login, así que el usuario se indica con el encabezado HTTP "X-Usuario-Id".
 * (Cuando se agregue login/token, solo cambia este servicio.)
 */
@Service
public class AccesoService {

    private final GenericDAO<Usuario> usuarioDAO = new GenericDAO<>(Usuario.class);

    /** Devuelve el usuario si existe y está activo. */
    public Usuario identificar(Integer idUsuario) {
        if (idUsuario == null) {
            throw new OperacionInvalidaException(
                    "Falta el encabezado X-Usuario-Id con el id del usuario que realiza la acción.");
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
