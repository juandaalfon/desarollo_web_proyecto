package api.controller;

import api.dto.Dtos.ProductoVendidoDTO;
import api.dto.Dtos.ReporteTiemposDTO;
import api.dto.Dtos.ReporteVentasDTO;
import api.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * RF-15: reportes del administrador (encabezado X-Usuario-Id de un administrador).
 * Parámetros opcionales: desde y hasta con formato 2026-10-01. Sin ellos, últimos 30 días.
 */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin
public class ReporteController {

    private static final String USUARIO = "X-Usuario-Id";

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/ventas")
    public ReporteVentasDTO ventas(
            @RequestHeader(value = USUARIO, required = false) Integer idUsuario,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.ventas(idUsuario, desde, hasta);
    }

    @GetMapping("/productos-mas-vendidos")
    public List<ProductoVendidoDTO> productosMasVendidos(
            @RequestHeader(value = USUARIO, required = false) Integer idUsuario,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "limite", required = false) Integer limite) {
        return servicio.productosMasVendidos(idUsuario, desde, hasta, limite);
    }

    @GetMapping("/tiempos-preparacion")
    public ReporteTiemposDTO tiemposPreparacion(
            @RequestHeader(value = USUARIO, required = false) Integer idUsuario,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.tiemposPreparacion(idUsuario, desde, hasta);
    }
}
