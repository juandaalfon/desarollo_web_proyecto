package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/** Entidad de la tabla "usuario". */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "usuario_login", nullable = false, length = 45, unique = true)
    private String usuarioLogin;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // se recibe en POST/PUT, nunca se devuelve en las respuestas
    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Usuario() {
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", nombre='" + nombre + "'" +
                ", usuarioLogin='" + usuarioLogin + "'" +
                ", rolId=" + (rol != null ? rol.getIdRol() : null) +
                ", activo=" + activo +
                "}";
    }

}
