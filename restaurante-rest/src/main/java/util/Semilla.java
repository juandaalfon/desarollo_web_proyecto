package util;

import jakarta.persistence.EntityManager;
import model.Categoria;
import model.Mesa;
import model.Producto;
import model.Rol;
import model.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

/**
 * Datos iniciales para poder probar de inmediato. Solo crea lo que no existe, así que es seguro
 * ejecutarla cada vez que arranca la API o la consola.
 *
 *   usuarios:  admin / admin123   (Administrador)
 *              mesero / mesero123 (Mesero)
 *              cocina / cocina123 (Cocina)
 *   catálogo:  5 mesas, 2 categorías y 4 productos (solo si esas tablas están vacías)
 *
 * Cambia estas claves antes de usar el sistema de verdad.
 */
public final class Semilla {

    private static final BCryptPasswordEncoder CIFRADOR = new BCryptPasswordEncoder();

    private Semilla() {
    }

    public static void ejecutar() {
        JPAUtil.enTransaccion(em -> {
            Rol admin = rol(em, "Administrador");
            Rol mesero = rol(em, "Mesero");
            Rol cocina = rol(em, "Cocina");

            usuario(em, "Administrador", "admin", "admin123", admin);
            usuario(em, "Mesero de prueba", "mesero", "mesero123", mesero);
            usuario(em, "Cocinero de prueba", "cocina", "cocina123", cocina);

            if (contar(em, "Mesa") == 0) {
                for (int i = 1; i <= 5; i++) {
                    Mesa m = new Mesa();
                    m.setNumero(i);
                    m.setCapacidad(i <= 3 ? 4 : 6);
                    m.setEstado("LIBRE");
                    em.persist(m);
                }
            }
            if (contar(em, "Producto") == 0 && contar(em, "Categoria") == 0) {
                Categoria platos = categoria(em, "Platos fuertes");
                Categoria bebidas = categoria(em, "Bebidas");
                producto(em, "Bandeja paisa", "Frijoles, arroz, carne, chicharrón y huevo", "32000", platos);
                producto(em, "Sancocho de gallina", "Sancocho tradicional", "28000", platos);
                producto(em, "Jugo natural", "Jugo en agua o leche", "8000", bebidas);
                producto(em, "Gaseosa", "Botella personal", "5000", bebidas);
            }
            return null;
        });
    }

    private static Rol rol(EntityManager em, String nombre) {
        return em.createQuery("SELECT r FROM Rol r WHERE r.nombreRol = ?1", Rol.class)
                .setParameter(1, nombre)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Rol r = new Rol();
                    r.setNombreRol(nombre);
                    em.persist(r);
                    return r;
                });
    }

    private static void usuario(EntityManager em, String nombre, String login, String clave, Rol rol) {
        boolean existe = em.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.usuarioLogin = ?1", Long.class)
                .setParameter(1, login)
                .getSingleResult() > 0;
        if (existe) {
            return;
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setUsuarioLogin(login);
        u.setContrasenaHash(CIFRADOR.encode(clave));
        u.setRol(rol);
        u.setActivo(true);
        em.persist(u);
    }

    private static Categoria categoria(EntityManager em, String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        em.persist(c);
        return c;
    }

    private static void producto(EntityManager em, String nombre, String descripcion, String precio,
                                 Categoria categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setCategoria(categoria);
        p.setDisponible(true);
        em.persist(p);
    }

    private static long contar(EntityManager em, String entidad) {
        return em.createQuery("SELECT COUNT(e) FROM " + entidad + " e", Long.class).getSingleResult();
    }
}
