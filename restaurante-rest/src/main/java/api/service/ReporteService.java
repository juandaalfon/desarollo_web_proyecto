package api.service;

import api.dto.Dtos.EtapaPromedioDTO;
import api.dto.Dtos.ProductoVendidoDTO;
import api.dto.Dtos.ReporteTiemposDTO;
import api.dto.Dtos.ReporteVentasDTO;
import api.dto.Dtos.VentaCategoriaDTO;
import api.dto.Dtos.VentaDiaDTO;
import api.exception.OperacionInvalidaException;
import dao.ReporteDAO;
import model.EstadoPedido;
import model.HistorialEstado;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * RF-15: reportes para el administrador. Una venta es un pedido ENTREGADO (los cancelados no cuentan).
 * Si no se indica rango, se usan los últimos 30 días (hasta hoy, inclusive).
 */
@Service
public class ReporteService {

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final AccesoService acceso;

    public ReporteService(AccesoService acceso) {
        this.acceso = acceso;
    }

    /** Rango de fechas [desde, hasta] inclusive, convertido a [inicio, fin) para consultar. */
    private record Rango(LocalDate desde, LocalDate hasta) {

        static Rango de(LocalDate desde, LocalDate hasta) {
            LocalDate fin = hasta != null ? hasta : LocalDate.now();
            LocalDate inicio = desde != null ? desde : fin.minusDays(29);
            if (inicio.isAfter(fin)) {
                throw new OperacionInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'.");
            }
            return new Rango(inicio, fin);
        }

        LocalDateTime inicio() {
            return desde.atStartOfDay();
        }

        LocalDateTime fin() {
            return hasta.plusDays(1).atStartOfDay();
        }
    }

    public ReporteVentasDTO ventas(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        acceso.exigirRol(idUsuario, TipoRol.ADMINISTRADOR);
        Rango rango = Rango.de(desde, hasta);

        BigDecimal total = BigDecimal.ZERO;
        int pedidos = 0;
        Map<LocalDate, BigDecimal> totalPorDia = new TreeMap<>();
        Map<LocalDate, Integer> pedidosPorDia = new TreeMap<>();
        for (Object[] fila : reporteDAO.ventasPorPedido(rango.inicio(), rango.fin())) {
            LocalDate dia = ((LocalDateTime) fila[1]).toLocalDate();
            BigDecimal valor = new BigDecimal(fila[2].toString());
            total = total.add(valor);
            pedidos++;
            totalPorDia.merge(dia, valor, BigDecimal::add);
            pedidosPorDia.merge(dia, 1, Integer::sum);
        }

        List<VentaDiaDTO> porDia = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : totalPorDia.entrySet()) {
            porDia.add(new VentaDiaDTO(e.getKey(), pedidosPorDia.get(e.getKey()), e.getValue().setScale(2, RoundingMode.HALF_UP)));
        }

        Map<String, long[]> unidadesPorCategoria = new LinkedHashMap<>();
        Map<String, BigDecimal> totalPorCategoria = new LinkedHashMap<>();
        for (Object[] fila : reporteDAO.productosVendidos(rango.inicio(), rango.fin())) {
            String categoria = String.valueOf(fila[2]);
            unidadesPorCategoria.computeIfAbsent(categoria, k -> new long[1])[0] += ((Number) fila[3]).longValue();
            totalPorCategoria.merge(categoria, new BigDecimal(fila[4].toString()), BigDecimal::add);
        }
        List<VentaCategoriaDTO> porCategoria = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : totalPorCategoria.entrySet()) {
            porCategoria.add(new VentaCategoriaDTO(e.getKey(), unidadesPorCategoria.get(e.getKey())[0],
                    e.getValue().setScale(2, RoundingMode.HALF_UP)));
        }
        porCategoria.sort((a, b) -> b.total().compareTo(a.total()));

        BigDecimal promedio = pedidos == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
        return new ReporteVentasDTO(rango.desde(), rango.hasta(), total.setScale(2, RoundingMode.HALF_UP), pedidos,
                promedio, porDia, porCategoria);
    }

    public List<ProductoVendidoDTO> productosMasVendidos(Integer idUsuario, LocalDate desde, LocalDate hasta,
                                                         Integer limite) {
        acceso.exigirRol(idUsuario, TipoRol.ADMINISTRADOR);
        int max = limite == null ? 10 : limite;
        if (max < 1 || max > 100) {
            throw new OperacionInvalidaException("El parámetro 'limite' debe estar entre 1 y 100.");
        }
        Rango rango = Rango.de(desde, hasta);
        List<ProductoVendidoDTO> lista = new ArrayList<>();
        for (Object[] fila : reporteDAO.productosVendidos(rango.inicio(), rango.fin())) {
            if (lista.size() == max) {
                break;
            }
            lista.add(new ProductoVendidoDTO((Integer) fila[0], String.valueOf(fila[1]), String.valueOf(fila[2]),
                    ((Number) fila[3]).longValue(), new BigDecimal(fila[4].toString()).setScale(2, RoundingMode.HALF_UP)));
        }
        return lista;
    }

    /** Tiempo promedio que los pedidos entregados pasaron en cada etapa (calculado con el historial). */
    public ReporteTiemposDTO tiemposPreparacion(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        acceso.exigirRol(idUsuario, TipoRol.ADMINISTRADOR);
        Rango rango = Rango.de(desde, hasta);

        Map<Integer, List<HistorialEstado>> porPedido = new LinkedHashMap<>();
        for (HistorialEstado h : reporteDAO.historialDeEntregados(rango.inicio(), rango.fin())) {
            porPedido.computeIfAbsent(h.getPedido().getIdPedido(), k -> new ArrayList<>()).add(h);
        }

        Map<EstadoPedido, Long> segundosPorEtapa = new EnumMap<>(EstadoPedido.class);
        Map<EstadoPedido, Integer> conteoPorEtapa = new EnumMap<>(EstadoPedido.class);
        long segundosTotales = 0;
        for (List<HistorialEstado> filas : porPedido.values()) {
            for (int i = 0; i + 1 < filas.size(); i++) {
                EstadoPedido etapa;
                try {
                    etapa = EstadoPedido.desdeTexto(filas.get(i).getEstado());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                long seg = Math.max(0, Duration.between(filas.get(i).getFechaHora(), filas.get(i + 1).getFechaHora()).getSeconds());
                segundosPorEtapa.merge(etapa, seg, Long::sum);
                conteoPorEtapa.merge(etapa, 1, Integer::sum);
            }
            segundosTotales += Math.max(0, Duration.between(filas.get(0).getFechaHora(),
                    filas.get(filas.size() - 1).getFechaHora()).getSeconds());
        }

        List<EtapaPromedioDTO> etapas = new ArrayList<>();
        for (Map.Entry<EstadoPedido, Long> e : segundosPorEtapa.entrySet()) {
            double promedio = e.getValue() / (double) conteoPorEtapa.get(e.getKey());
            etapas.add(new EtapaPromedioDTO(e.getKey().name(), redondear(promedio), redondear(promedio / 60.0)));
        }
        double totalPromedioMin = porPedido.isEmpty() ? 0 : redondear(segundosTotales / (double) porPedido.size() / 60.0);
        return new ReporteTiemposDTO(rango.desde(), rango.hasta(), porPedido.size(), etapas, totalPromedioMin);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
