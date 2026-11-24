// aporte de la web get y patch preparacion
import { api } from '../api.js';
import { h, vaciar, intentar, vacio, hora, minutosDesde } from '../ui.js';

const LIMITE_TARDE_MIN = 15;

export async function vistaCocina(cont) {
  const pendientes = h('div', { class: 'stack' });
  const enPrep = h('div', { class: 'stack' });
  const cuentaP = h('span', { class: 'count' }, '0');
  const cuentaE = h('span', { class: 'count' }, '0');
  const reloj = h('span', { class: 'small muted' });

  cont.append(
    h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, 'Cocina'), h('p', { class: 'muted' }, 'Se actualiza sola cada 10 segundos.')), reloj),
    h('div', { class: 'board' },
      h('section', { class: 'col' }, h('h2', {}, 'Pendientes ', cuentaP), pendientes),
      h('section', { class: 'col' }, h('h2', {}, 'En preparación ', cuentaE), enPrep)));

  const cargar = async () => {
    const [cola, preparando] = await Promise.all([
      api.get('/api/cocina/pedidos/pendientes'),
      api.get('/api/pedidos?estado=EN_PREPARACION'),
    ]);
    // los pedidos en preparación no vienen con sus productos: se piden aparte
    const detalles = await Promise.all(preparando.map((p) => api.get(`/api/pedidos/${p.idPedido}/detalles`)));
    preparando.sort((a, b) => new Date(a.fechaCreacion) - new Date(b.fechaCreacion));

    cuentaP.textContent = cola.length;
    cuentaE.textContent = preparando.length;
    vaciar(pendientes).append(...(cola.length ? cola.map((p) => ticket({
      id: p.idPedido, mesa: p.numeroMesa, personas: p.numPersonas, mesero: p.mesero, desde: p.fechaCreacion,
      items: p.items.map((i) => ({ cantidad: i.cantidad, nombre: i.producto, notas: i.notas })),
      boton: ['▶ Iniciar preparación', 'primary', 'EN_PREPARACION'],
    }, cargar)) : [vacio('Sin pedidos pendientes. 🎉')]));
    vaciar(enPrep).append(...(preparando.length ? preparando.map((p, i) => ticket({
      id: p.idPedido, mesa: p.mesa?.numero, personas: p.numPersonas, mesero: p.mesero?.nombre, desde: p.fechaCreacion, prep: true,
      items: detalles[i].map((d) => ({ cantidad: d.cantidad, nombre: d.producto?.nombre, notas: d.notas })),
      boton: ['✓ Marcar listo', 'ok', 'LISTO'],
    }, cargar)) : [vacio('Nada en preparación.')]));
    reloj.textContent = 'Actualizado ' + hora(new Date().toISOString());
  };

  await cargar();
  const t = setInterval(() => cargar().catch(() => {}), 10000);
  return () => clearInterval(t);
}

function ticket(p, recargar) {
  const min = minutosDesde(p.desde);
  const tarde = min >= LIMITE_TARDE_MIN;
  return h('article', { class: 'card ticket' + (p.prep ? ' prep' : '') + (tarde ? ' late' : '') },
    h('div', { class: 'row between' },
      h('a', { href: '#/pedidos/' + p.id, style: 'color:inherit;text-decoration:none' },
        h('h3', {}, `Mesa ${p.mesa ?? '?'} · #${p.id}`)),
      h('span', { class: 'timer' + (tarde ? ' late' : '') }, `${min} min`)),
    h('div', { class: 'small muted' }, `${p.personas ?? '?'} persona(s) · ${p.mesero ?? '—'}`),
    p.items.length
      ? h('ul', {}, p.items.map((i) => h('li', {}, h('b', {}, i.cantidad + '×'), ' ' + i.nombre,
        i.notas ? h('div', { class: 'note' }, '📝 ' + i.notas) : null)))
      : h('p', { class: 'muted small', style: 'margin:10px 0' }, 'Sin productos (no se puede iniciar).'),
    h('button', { class: 'btn lg ' + p.boton[1], type: 'button', style: 'width:100%', disabled: !p.items.length && !p.prep,
      onclick: async () => { if (await intentar(() => api.patch(`/api/pedidos/${p.id}/estado`, { estado: p.boton[2] }))) recargar(); } }, p.boton[0]));
}
