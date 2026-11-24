package api.controller;

import api.dto.Dtos.EstadoMesaRequest;
import api.service.MesaService;
import model.Mesa;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-13: CRUD de mesas (heredado) y cambio de estado. */
@RestController
@RequestMapping("/api/mesas")
public class MesaController extends CrudController<Mesa> {

    private final MesaService servicio;

    public MesaController(MesaService servicio) {
        super(servicio);
        this.servicio = servicio;
    }

    /** Body: {"estado": "OCUPADA"}. Valores: LIBRE, RESERVADA, OCUPADA. */
    @PatchMapping("/{id}/estado")
    public Mesa cambiarEstado(@PathVariable("id") Integer id, @RequestBody EstadoMesaRequest cuerpo) {
        return servicio.cambiarEstado(id, cuerpo.estado());
    }
}
