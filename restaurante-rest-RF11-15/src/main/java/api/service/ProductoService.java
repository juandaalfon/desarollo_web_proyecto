package api.service;

import api.exception.OperacionInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.GenericDAO;
import model.Categoria;
import model.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/** RF-11 (menú: productos y precios) y RF-12 (disponibilidad). */
@Service
public class ProductoService extends CrudService<Producto> {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    private final GenericDAO<Categoria> categoriaDAO = new GenericDAO<>(Categoria.class);

    public ProductoService(ObjectMapper mapper) {
        super(Producto.class, mapper);
    }

    @Override
    protected void validar(Producto producto) {
        producto.setNombre(Validar.texto(producto.getNombre(), "nombre", 45));
        producto.setDescripcion(Validar.textoOpcional(producto.getDescripcion(), "descripcion", 255));

        BigDecimal precio = producto.getPrecio();
        if (precio == null || precio.signum() <= 0) {
            throw new OperacionInvalidaException("El precio es obligatorio y debe ser mayor que 0.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new OperacionInvalidaException("El precio no puede superar " + PRECIO_MAXIMO + ".");
        }
        producto.setPrecio(precio.setScale(2, RoundingMode.HALF_UP));

        if (producto.getCategoria() == null || producto.getCategoria().getIdCategoria() == null) {
            throw new OperacionInvalidaException("Debes indicar la categoría: \"categoria\": {\"idCategoria\": 1}.");
        }
        Categoria categoria = categoriaDAO.buscarPorId(producto.getCategoria().getIdCategoria());
        if (categoria == null) {
            throw new OperacionInvalidaException(
                    "La categoría con id " + producto.getCategoria().getIdCategoria() + " no existe.");
        }
        producto.setCategoria(categoria);

        if (producto.getDisponible() == null) {
            producto.setDisponible(Boolean.TRUE);
        }
    }

    /** RF-12: marca un producto como disponible o no disponible. */
    public Producto cambiarDisponibilidad(Integer id, Boolean disponible) {
        if (disponible == null) {
            throw new OperacionInvalidaException("Debes enviar \"disponible\": true o false.");
        }
        Producto producto = buscar(id);
        producto.setDisponible(disponible);
        if (!dao.actualizar(producto)) {
            throw new OperacionInvalidaException("No se pudo actualizar la disponibilidad del producto.");
        }
        return buscar(id);
    }

    /** Productos que se pueden pedir ahora. */
    public List<Producto> disponibles() {
        return dao.listar().stream()
                .filter(p -> Boolean.TRUE.equals(p.getDisponible()))
                .collect(Collectors.toList());
    }
}
