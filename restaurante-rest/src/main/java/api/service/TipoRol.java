package api.service;

import util.Texto;

/** Tipos de rol del enunciado. Se reconocen por el nombre del rol en la base de datos. */
public enum TipoRol {
    MESERO, COCINA, ADMINISTRADOR;

    /** "Mesero", "Cocina/Cocinero", "Administrador/Admin"... Devuelve null si no coincide con ninguno. */
    public static TipoRol de(String nombreRol) {
        String n = Texto.normalizar(nombreRol);
        if (n.contains("ADMIN")) {
            return ADMINISTRADOR;
        }
        if (n.contains("MESER")) {
            return MESERO;
        }
        if (n.contains("COCIN")) {
            return COCINA;
        }
        return null;
    }
}
