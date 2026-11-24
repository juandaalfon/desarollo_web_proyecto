// RF-15: reportes de ventas, productos más vendidos y tiempos de preparación.
import { api } from '../api.js';
import { h, vaciar, dinero, campo, intentar, vacio } from '../ui.js';

const iso = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);

export async function vistaResumen(cont) {
  const hoy = new Date();
  const hace30 = new Date(hoy.getTime() - 29 * 86400000);
  const desde = h('input', { type: 'date', value: iso(hace30) });
  const hasta = h('input', { type: 'date', value: iso(hoy) });
  const resultado = h('div', { class: 'stack', style: 'gap:18px' });

  const cargar = async () => {
    const q = `?desde=${desde.value}&hasta=${hasta.value}`;
    let ventas, top, tiempos;
    const ok = await intentar(async () => {
      [ventas, top, tiempos] = await Promise.all([
        api.get('/api/reportes/ventas' + q), api.get('/api/reportes/productos-mas-vendidos' + q + '&limite=8'),
        api.get('/api/reportes/tiempos-preparacion' + q)]);
    });
    if (!ok) return;
    vaciar(resultado).append(
      h('div', { class: 'grid kpi' },
        kpi('Total vendido', dinero(ventas.totalVentas)), kpi('Pedidos entregados', ventas.numeroPedidos),
        kpi('Ticket promedio', dinero(ventas.ticketPromedio)), kpi('Tiempo total promedio', (tiempos.tiempoTotalPromedioMinutos ?? 0).toFixed(1) + ' min')),
      h('div', { class: 'cols-2' },
        panel('Ventas por día', barras(ventas.ventasPorDia, (v) => v.fecha.slice(5), (v) => Number(v.total), (v) => dinero(v.total))),
        panel('Ventas por categoría', barras(ventas.ventasPorCategoria, (v) => v.categoria, (v) => Number(v.total), (v) => dinero(v.total)))),
      h('div', { class: 'cols-2' },
        panel('Productos más vendidos', barras(top, (p) => p.producto, (p) => p.unidades, (p) => `${p.unidades} u.`)),
        panel('Tiempo promedio por etapa', barras(tiempos.etapas, (e) => e.etapa.replaceAll('_', ' ').toLowerCase(), (e) => e.promedioMinutos, (e) => e.promedioMinutos.toFixed(1) + ' min', true))),
      h('p', { class: 'small muted' }, `Pedidos analizados para tiempos: ${tiempos.pedidosAnalizados}. Una venta es un pedido ENTREGADO; los cancelados no cuentan.`));
  };

  cont.append(
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Resumen'), h('p', { class: 'muted' }, 'Reportes de ventas y tiempos.')),
      h('div', { class: 'row', style: 'align-items:flex-end' }, campo('Desde', desde), campo('Hasta', hasta),
        h('button', { class: 'btn primary', type: 'button', onclick: cargar }, 'Actualizar'))),
    resultado);
  await cargar();
}

const kpi = (label, valor) => h('div', { class: 'card' }, h('div', { class: 'label' }, label), h('div', { class: 'value' }, valor));
const panel = (titulo, contenido) => h('div', { class: 'card' }, h('h3', { style: 'margin-bottom:12px' }, titulo), contenido);

function barras(datos, etiqueta, valor, texto, alt = false) {
  if (!datos?.length) return vacio('Sin datos en este periodo.');
  const max = Math.max(...datos.map(valor), 1);
  return h('div', { class: 'bars' }, datos.map((d) => h('div', { class: 'bar-row' },
    h('span', { title: etiqueta(d), style: 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap' }, etiqueta(d)),
    h('div', { class: 'bar-track' }, h('div', { class: 'bar-fill' + (alt ? ' alt' : ''), style: `width:${(valor(d) / max) * 100}%` })),
    h('span', { class: 'right mono' }, texto(d)))));
}
