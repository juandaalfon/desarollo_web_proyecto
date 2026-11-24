package api.controller;

import api.dto.Dtos.DisponibilidadRequest;
import api.service.ProductoService;
import model.Producto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** RF-11 (CRUD del menú, heredado) y RF-12 (disponibilidad). */
@RestController
@RequestMapping("/api/productos")
public class ProductoController extends CrudController<Producto> {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        super(servicio);
        this.servicio = servicio;
    }

    /** Productos que se pueden pedir ahora. */
    @GetMapping("/disponibles")
    public List<Producto> disponibles() {
        return servicio.disponibles();
    }

    /** RF-12: Body: {"disponible": false}. */
    @PatchMapping("/{id}/disponibilidad")
    public Producto cambiarDisponibilidad(@PathVariable("id") Integer id, @RequestBody DisponibilidadRequest cuerpo) {
        return servicio.cambiarDisponibilidad(id, cuerpo.disponible());
    }
}
