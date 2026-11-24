// Facturas generadas (se crean desde el detalle de un pedido ENTREGADO).
import { h, dinero, fechaHora } from '../ui.js';
import { tablaCrud } from './crud.js';

export async function vistaFacturas(cont) {
  cont.append(h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Facturas'),
    h('p', { class: 'muted' }, 'Para generar una, abre un pedido entregado y pulsa «Generar factura».'))));
  const caja = h('div', {});
  cont.append(caja);
  await tablaCrud(caja, {
    titulo: 'Facturas', singular: 'factura', endpoint: '/api/facturas', idKey: 'idFactura', sinCrear: true,
    orden: (a, b) => b.idFactura - a.idFactura, descripcion: (x) => `la factura #${x.idFactura}`,
    columnas: [
      { texto: 'N.º', celda: (x) => '#' + x.idFactura },
      { texto: 'Pedido', celda: (x) => h('a', { href: '#/pedidos/' + x.pedido?.idPedido }, '#' + x.pedido?.idPedido) },
      { texto: 'Fecha', celda: (x) => fechaHora(x.fechaGeneracion) },
      { texto: 'Generada por', celda: (x) => x.usuarioGenera?.nombre ?? '—' },
      { texto: 'Total', num: true, celda: (x) => dinero(x.total) },
    ],
    campos: [{ nombre: 'total', etiqueta: 'Total', tipo: 'number', requerido: true }],
  });
}
