package api.service;

import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import model.Rol;
import model.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** RF-14: gestión de usuarios (login único, rol válido y contraseña guardada como hash BCrypt). */
@Service
public class UsuarioService extends CrudService<Usuario> {

    private final GenericDAO<Rol> rolDAO = new GenericDAO<>(Rol.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioService(ObjectMapper mapper) {
        super(Usuario.class, mapper);
    }

    @Override
    protected void validar(Usuario usuario) {
        usuario.setNombre(Validar.texto(usuario.getNombre(), "nombre", 100));
        usuario.setUsuarioLogin(Validar.texto(usuario.getUsuarioLogin(), "usuarioLogin", 45));

        for (Usuario otro : dao.listar()) {
            if (otro.getUsuarioLogin().equalsIgnoreCase(usuario.getUsuarioLogin())
                    && !Objects.equals(otro.getIdUsuario(), usuario.getIdUsuario())) {
                throw new ConflictoException("Ya existe un usuario con el login '" + usuario.getUsuarioLogin() + "'.");
            }
        }

        if (usuario.getRol() == null || usuario.getRol().getIdRol() == null) {
            throw new OperacionInvalidaException("Debes indicar el rol: \"rol\": {\"idRol\": 1}.");
        }
        Rol rol = rolDAO.buscarPorId(usuario.getRol().getIdRol());
        if (rol == null) {
            throw new OperacionInvalidaException("El rol con id " + usuario.getRol().getIdRol() + " no existe.");
        }
        usuario.setRol(rol);

        if (usuario.getActivo() == null) {
            usuario.setActivo(Boolean.TRUE);
        }
        guardarComoHash(usuario);
    }

    /** La contraseña llega en texto (o ya como hash BCrypt) y siempre se guarda como hash. */
    private void guardarComoHash(Usuario usuario) {
        String clave = usuario.getContrasenaHash();
        if (clave == null || clave.isBlank()) {
            throw new OperacionInvalidaException("La contraseña es obligatoria (campo 'contrasenaHash').");
        }
        boolean yaEsHash = clave.startsWith("$2a$") || clave.startsWith("$2b$") || clave.startsWith("$2y$");
        if (yaEsHash) {
            return;
        }
        if (clave.length() < 4) {
            throw new OperacionInvalidaException("La contraseña debe tener al menos 4 caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new OperacionInvalidaException("La contraseña no puede superar 72 bytes.");
        }
        usuario.setContrasenaHash(encoder.encode(clave));
    }
}
