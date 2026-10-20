package api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Objetos que entran y salen de la API en los requerimientos RF-06 a RF-10. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Entrada ----------
    public record CambioEstadoRequest(String estado) {
    }

    // ---------- RF-06: cola de cocina ----------
    public record ItemCocinaDTO(Integer idDetalle, String producto, Integer cantidad, String notas) {
    }

    public record PedidoCocinaDTO(Integer idPedido, Integer numeroMesa, String mesero, Integer numPersonas,
                                  String estado, LocalDateTime fechaCreacion, long minutosEnEspera,
                                  List<ItemCocinaDTO> items) {
    }

    // ---------- RF-10: historial ----------
    /** duracionSegundos = tiempo que el pedido estuvo en ese estado (null si es el estado actual). */
    public record EtapaDTO(String estado, LocalDateTime fechaHora, Integer idUsuario, String usuario,
                           Long duracionSegundos) {
    }

    public record HistorialPedidoDTO(Integer idPedido, String estadoActual, boolean finalizado,
                                     long tiempoTotalSegundos, List<EtapaDTO> etapas) {
    }
}
