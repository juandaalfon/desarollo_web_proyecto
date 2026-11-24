package security;

import api.service.TipoRol;

/**
 * Qué puede hacer cada rol sobre cada ruta de la API (RF-01: "acceden según su rol").
 * Es el primer filtro (lo aplica AuthInterceptor); los servicios vuelven a validar el rol con AccesoService
 * y, además, que un mesero solo toque sus propios pedidos.
 *
 * | Recurso                              | Administrador | Mesero                          | Cocina                        |
 * |--------------------------------------|---------------|---------------------------------|-------------------------------|
 * | roles, usuarios (RF-14)              | todo          | nada                            | nada                          |
 * | mesas (RF-13), categorias, productos | todo          | solo consultar                  | solo consultar                |
 * |   (RF-11, RF-12)                     |               |                                 |                               |
 * | historial-estados (RF-10)            | consultar     | consultar                       | consultar                     |
 * | pedidos (RF-02, 03, 07, 08, 09)      | todo          | consultar y gestionar los suyos | consultar y pasar a           |
 * |                                      |               |                                 | EN_PREPARACION / LISTO        |
 * | detalles-pedido (RF-04, RF-05)       | todo          | consultar y gestionar los suyos | solo consultar                |
 * | cocina (RF-06)                       | todo          | nada                            | todo                          |
 * | reportes (RF-15), facturas           | todo          | nada                            | nada                          |
 */
public final class Permisos {

    private Permisos() {
    }

    public static boolean permitido(TipoRol rol, String metodo, String ruta) {
        if (rol == TipoRol.ADMINISTRADOR) {
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
            case "cocina":
                return rol == TipoRol.COCINA;
            default:
                return false; // roles, usuarios, reportes, facturas: solo administrador
        }
    }
}
