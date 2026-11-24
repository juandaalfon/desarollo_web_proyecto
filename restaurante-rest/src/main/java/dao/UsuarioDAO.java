package dao;

import model.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * DAO de Usuario: antes de guardar cifra la contraseña con BCrypt (RF-01).
 * Así la clave nunca queda en texto plano, se cree el usuario por la consola, por REST o por la semilla.
 */
public class UsuarioDAO extends GenericDAO<Usuario> {

    private static final BCryptPasswordEncoder CIFRADOR = new BCryptPasswordEncoder();

    public UsuarioDAO() {
        super(Usuario.class);
    }

    @Override
    public boolean crear(Usuario usuario) {
        cifrar(usuario);
        return super.crear(usuario);
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        cifrar(usuario);
        return super.actualizar(usuario);
    }

    /** Cifra solo si la contraseña todavía no es un hash BCrypt (los hashes BCrypt empiezan por "$2"). */
    private void cifrar(Usuario usuario) {
        String clave = usuario.getContrasenaHash();
        if (clave != null && !clave.startsWith("$2")) {
            usuario.setContrasenaHash(CIFRADOR.encode(clave));
        }
    }
}
