package util;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de texto: permite aceptar "En preparación", "EN PREPARACION", "en_preparacion", etc. */
public final class Texto {

    private Texto() {
    }

    /** Quita tildes, pasa a mayúsculas y cambia espacios/guiones por "_". */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String s = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return s.replaceAll("^_+|_+$", "");
    }
}
