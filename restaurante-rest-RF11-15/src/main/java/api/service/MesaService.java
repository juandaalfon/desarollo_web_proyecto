package api.service;

import api.exception.ConflictoException;
import api.exception.OperacionInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.EstadoMesa;
import model.Mesa;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** RF-13: mesas del restaurante y su estado (libre, reservada, ocupada). */
@Service
public class MesaService extends CrudService<Mesa> {

    public MesaService(ObjectMapper mapper) {
        super(Mesa.class, mapper);
    }

    @Override
    protected void validar(Mesa mesa) {
        if (mesa.getNumero() == null || mesa.getNumero() <= 0) {
            throw new OperacionInvalidaException("El número de mesa es obligatorio y debe ser mayor que 0.");
        }
        if (mesa.getCapacidad() == null || mesa.getCapacidad() <= 0) {
            throw new OperacionInvalidaException("La capacidad es obligatoria y debe ser mayor que 0.");
        }
        EstadoMesa estado = EstadoMesa.LIBRE; // si no se envía, la mesa nace libre
        if (mesa.getEstado() != null && !mesa.getEstado().isBlank()) {
            estado = parsear(mesa.getEstado());
        }
        mesa.setEstado(estado.name());

        for (Mesa otra : dao.listar()) {
            if (Objects.equals(otra.getNumero(), mesa.getNumero())
                    && !Objects.equals(otra.getIdMesa(), mesa.getIdMesa())) {
                throw new ConflictoException("Ya existe la mesa número " + mesa.getNumero() + ".");
            }
        }
    }

    /** Cambia solo el estado de la mesa. */
    public Mesa cambiarEstado(Integer id, String estadoTexto) {
        EstadoMesa estado = parsear(estadoTexto);
        Mesa mesa = buscar(id);
        mesa.setEstado(estado.name());
        if (!dao.actualizar(mesa)) {
            throw new OperacionInvalidaException("No se pudo cambiar el estado de la mesa.");
        }
        return buscar(id);
    }

    private EstadoMesa parsear(String texto) {
        try {
            return EstadoMesa.desdeTexto(texto);
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException(e.getMessage());
        }
    }
}
