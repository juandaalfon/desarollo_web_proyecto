package model;

import util.Texto;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Estados de un pedido y las transiciones permitidas (flujo del enunciado):
 *
 *   PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
 *   CANCELADO solo desde PENDIENTE o EN_PREPARACION
 */
public enum EstadoPedido {
    PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO;

    /** Estados a los que se puede pasar desde este. */
    public Set<EstadoPedido> siguientes() {
        switch (this) {
            case PENDIENTE:
                return EnumSet.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION:
                return EnumSet.of(LISTO, CANCELADO);
            case LISTO:
                return EnumSet.of(ENTREGADO);
            default:
                return EnumSet.noneOf(EstadoPedido.class);
        }
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return siguientes().contains(destino);
    }

    /** ENTREGADO y CANCELADO ya no cambian. */
    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    /** Mensaje claro cuando la transición no es válida. */
    public String mensajeTransicion(EstadoPedido destino) {
        if (destino == CANCELADO) {
            return "Solo se puede cancelar un pedido PENDIENTE o EN_PREPARACION (estado actual: " + this + ").";
        }
        if (esFinal()) {
            return "El pedido ya está " + this + " y no admite más cambios.";
        }
        String permitidos = siguientes().stream().map(Enum::name).collect(Collectors.joining(", "));
        return "No se puede pasar de " + this + " a " + destino + ". Desde " + this + " solo se permite: " + permitidos + ".";
    }

    /** Convierte texto del usuario al estado. Lanza IllegalArgumentException si no existe. */
    public static EstadoPedido desdeTexto(String texto) {
        String n = Texto.normalizar(texto);
        switch (n) {
            case "PENDIENTE":
                return PENDIENTE;
            case "EN_PREPARACION":
            case "PREPARACION":
                return EN_PREPARACION;
            case "LISTO":
                return LISTO;
            case "ENTREGADO":
                return ENTREGADO;
            case "CANCELADO":
            case "CANCELADA":
                return CANCELADO;
            default:
                throw new IllegalArgumentException("Estado de pedido inválido: '" + texto
                        + "'. Valores permitidos: PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO.");
        }
    }
}
