package api.controller;

import api.dto.Dtos.PedidoCocinaDTO;
import api.service.PedidoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import security.Sesion;

import java.util.List;

/** RF-06: vista de cocina. La sesión debe ser de un usuario de cocina (o administrador). */
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
    public List<PedidoCocinaDTO> colaPendientes(HttpServletRequest peticion) {
        Sesion sesion = Sesion.de(peticion);
        return servicio.colaCocina(sesion != null ? sesion.idUsuario() : null);
    }
}
