package api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objetos que entran y salen de la API en los requerimientos RF-06 a RF-15. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Entrada ----------
    public record CambioEstadoRequest(String estado) {
    }

    public record DisponibilidadRequest(Boolean disponible) {
    }

    public record EstadoMesaRequest(String estado) {
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

    // ---------- RF-15: reportes ----------
    public record VentaDiaDTO(LocalDate fecha, int pedidos, BigDecimal total) {
    }

    public record VentaCategoriaDTO(String categoria, long unidades, BigDecimal total) {
    }

    public record ReporteVentasDTO(LocalDate desde, LocalDate hasta, BigDecimal totalVentas, int numeroPedidos,
                                   BigDecimal ticketPromedio, List<VentaDiaDTO> ventasPorDia,
                                   List<VentaCategoriaDTO> ventasPorCategoria) {
    }

    public record ProductoVendidoDTO(Integer idProducto, String producto, String categoria, long unidades,
                                     BigDecimal total) {
    }

    public record EtapaPromedioDTO(String etapa, double promedioSegundos, double promedioMinutos) {
    }

    public record ReporteTiemposDTO(LocalDate desde, LocalDate hasta, int pedidosAnalizados,
                                    List<EtapaPromedioDTO> etapas, double tiempoTotalPromedioMinutos) {
    }
}
