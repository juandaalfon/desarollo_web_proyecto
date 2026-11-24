// aporte de la web get, delete y put producto
import { api } from '../api.js';
import { h, dinero, si, intentar } from '../ui.js';
import { tablaCrud } from './crud.js';

export async function vistaMenu(cont) {
  cont.append(h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Menú'), h('p', { class: 'muted' }, 'Productos, precios, disponibilidad y categorías.'))));
  const productos = h('div', {});
  const categorias = h('div', { style: 'margin-top:32px' });
  cont.append(productos, categorias);

  await tablaCrud(productos, {
    titulo: 'Productos', singular: 'producto', endpoint: '/api/productos', idKey: 'idProducto',
    orden: (a, b) => (a.categoria?.nombre ?? '').localeCompare(b.categoria?.nombre ?? '') || a.nombre.localeCompare(b.nombre),
    descripcion: (p) => `el producto "${p.nombre}"`,
    columnas: [
      { texto: 'Producto', celda: (p) => h('div', {}, h('b', {}, p.nombre), p.descripcion ? h('div', { class: 'small muted' }, p.descripcion) : null) },
      { texto: 'Categoría', celda: (p) => p.categoria?.nombre ?? '—' },
      { texto: 'Precio', num: true, celda: (p) => dinero(p.precio) },
      { texto: 'Disponible', celda: (p) => si(p.disponible) },
    ],
    acciones: (p, recargar) => [h('button', { class: 'btn sm', type: 'button', onclick: async () => {
      if (await intentar(() => api.patch(`/api/productos/${p.idProducto}/disponibilidad`, { disponible: !p.disponible }),
        p.disponible ? 'Marcado como agotado' : 'Marcado como disponible')) await recargar();
    } }, p.disponible ? 'Agotar' : 'Habilitar'), ' '],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
      { nombre: 'descripcion', etiqueta: 'Descripción', tipo: 'textarea' },
      { nombre: 'precio', etiqueta: 'Precio', tipo: 'number', requerido: true },
      { nombre: 'categoria', etiqueta: 'Categoría', tipo: 'select', fk: 'idCategoria', requerido: true,
        opciones: async () => (await api.get('/api/categorias')).map((c) => ({ valor: c.idCategoria, texto: c.nombre })) },
      { nombre: 'disponible', etiqueta: 'Disponible para pedir', tipo: 'checkbox', valorInicial: true },
    ],
  });

  await tablaCrud(categorias, {
    titulo: 'Categorías', singular: 'categoría', endpoint: '/api/categorias', idKey: 'idCategoria',
    orden: (a, b) => a.nombre.localeCompare(b.nombre), descripcion: (c) => `la categoría "${c.nombre}"`,
    columnas: [{ texto: 'Nombre', celda: (c) => c.nombre }],
    campos: [{ nombre: 'nombre', etiqueta: 'Nombre', requerido: true }],
  });
}
