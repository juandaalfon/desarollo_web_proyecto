package service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Estados de pedido y de mesa, y qué cambios de estado están permitidos. */
public final class Estados {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PREPARACION = "EN_PREPARACION";
    public static final String LISTO = "LISTO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String CANCELADO = "CANCELADO";

    public static final String MESA_LIBRE = "LIBRE";
    public static final String MESA_OCUPADA = "OCUPADA";

    /** PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO; CANCELADO desde cualquier estado no final. */
    private static final Map<String, List<String>> TRANSICIONES = Map.of(
            PENDIENTE, List.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, List.of(LISTO, CANCELADO),
            LISTO, List.of(ENTREGADO, CANCELADO),
            ENTREGADO, List.of(),
            CANCELADO, List.of());

    private Estados() {
    }

    /** "En preparación", "en_preparacion" y "EN PREPARACION" quedan como EN_PREPARACION. */
    public static String normalizar(String estado) {
        if (estado == null) {
            return null;
        }
        return Normalizer.normalize(estado.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');
    }

    public static boolean esFinal(String estado) {
        return ENTREGADO.equals(normalizar(estado)) || CANCELADO.equals(normalizar(estado));
    }

    public static boolean existe(String estado) {
        return TRANSICIONES.containsKey(normalizar(estado));
    }

    public static boolean transicionValida(String desde, String hacia) {
        List<String> permitidos = TRANSICIONES.get(normalizar(desde));
        return permitidos != null && permitidos.contains(normalizar(hacia));
    }
}
