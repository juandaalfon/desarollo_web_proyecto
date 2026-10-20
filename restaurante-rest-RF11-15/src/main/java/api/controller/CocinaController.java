package api.controller;

import api.dto.Dtos.PedidoCocinaDTO;
import api.service.PedidoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** RF-06: vista de cocina. Requiere el encabezado X-Usuario-Id de un usuario de cocina (o administrador). */
@RestController
@RequestMapping("/api/cocina")
@CrossOrigin
public class CocinaController {

    private final PedidoService servicio;

    public CocinaController(PedidoService servicio) {
        this.servicio = servicio;
    }

    /** Cola de pedidos PENDIENTES, del más antiguo al más reciente, con sus productos y observaciones. */
    @GetMapping("/pedidos/pendientes")
    public List<PedidoCocinaDTO> colaPendientes(
            @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
        return servicio.colaCocina(idUsuario);
    }
}
