package security;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Los 3 tipos de usuario del sistema (RF-01). Se reconocen por el nombre guardado en la tabla rol:
 * "Administrador", "Administración"... -> ADMINISTRACION, "Mesero" -> MESERO, "Cocina"/"Cocinero" -> COCINA.
 */
public enum TipoRol {
    ADMINISTRACION,
    MESERO,
    COCINA;

    /** Devuelve el tipo que corresponde al nombre del rol, o null si el rol no es ninguno de los tres. */
    public static TipoRol deNombre(String nombreRol) {
        if (nombreRol == null) {
            return null;
        }
        String n = Normalizer.normalize(nombreRol.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (n.startsWith("admin")) {
            return ADMINISTRACION;
        }
        if (n.startsWith("mesero")) {
            return MESERO;
        }
        if (n.startsWith("cocin")) {
            return COCINA;
        }
        return null;
    }
}
