package model;

import util.Texto;

/** Estados de una mesa (RF-13): libre, reservada y ocupada. */
public enum EstadoMesa {
    LIBRE, RESERVADA, OCUPADA;

    /** Acepta también "reservado" y "ocupado". Lanza IllegalArgumentException si no existe. */
    public static EstadoMesa desdeTexto(String texto) {
        switch (Texto.normalizar(texto)) {
            case "LIBRE":
                return LIBRE;
            case "RESERVADA":
            case "RESERVADO":
                return RESERVADA;
            case "OCUPADA":
            case "OCUPADO":
                return OCUPADA;
            default:
                throw new IllegalArgumentException("Estado de mesa inválido: '" + texto
                        + "'. Valores permitidos: LIBRE, RESERVADA, OCUPADA.");
        }
    }
}
