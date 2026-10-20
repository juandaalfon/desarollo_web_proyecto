package security;

/**
 * Qué puede hacer cada rol sobre cada ruta de la API (RF-01: "acceden según su rol").
 *
 * | Recurso                         | Administración | Mesero                    | Cocina                    |
 * |---------------------------------|----------------|---------------------------|---------------------------|
 * | roles, usuarios                 | todo           | nada                      | nada                      |
 * | mesas, categorias, productos    | todo           | solo consultar            | solo consultar            |
 * | historial-estados               | todo           | solo consultar            | solo consultar            |
 * | pedidos                         | todo           | consultar y gestionar     | consultar y cambiar estado|
 * | detalles-pedido                 | todo           | consultar y gestionar     | solo consultar            |
 *
 * Además, los servicios vuelven a validar el rol y que el pedido sea del mesero (por si se usan desde la consola).
 */
public final class Permisos {

    private Permisos() {
    }

    public static boolean permitido(TipoRol rol, String metodo, String ruta) {
        if (rol == TipoRol.ADMINISTRACION) {
            return true;
        }
        String limpia = ruta.endsWith("/") ? ruta.substring(0, ruta.length() - 1) : ruta;
        String[] partes = limpia.split("/"); // ["", "api", "recurso", ...]
        String recurso = partes.length > 2 ? partes[2] : "";
        boolean lectura = "GET".equalsIgnoreCase(metodo);

        switch (recurso) {
            case "auth":
                return true;
            case "mesas":
            case "categorias":
            case "productos":
            case "historial-estados":
                return lectura;
            case "pedidos":
                if (lectura) {
                    return true;
                }
                if (limpia.endsWith("/estado")) {
                    return rol == TipoRol.MESERO || rol == TipoRol.COCINA;
                }
                return rol == TipoRol.MESERO;
            case "detalles-pedido":
                return lectura || rol == TipoRol.MESERO;
            default:
                return false; // roles y usuarios: solo administración
        }
    }
}
