package api.service;

import api.exception.ConflictoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Categoria;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** RF-11: categorías del menú (nombre obligatorio y sin repetir). */
@Service
public class CategoriaService extends CrudService<Categoria> {

    public CategoriaService(ObjectMapper mapper) {
        super(Categoria.class, mapper);
    }

    @Override
    protected void validar(Categoria categoria) {
        categoria.setNombre(Validar.texto(categoria.getNombre(), "nombre", 45));
        for (Categoria otra : dao.listar()) {
            if (otra.getNombre().equalsIgnoreCase(categoria.getNombre())
                    && !Objects.equals(otra.getIdCategoria(), categoria.getIdCategoria())) {
                throw new ConflictoException("Ya existe la categoría '" + otra.getNombre() + "'.");
            }
        }
    }
}
