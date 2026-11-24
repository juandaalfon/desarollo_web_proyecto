// RF-13: estado de las mesas. El administrador además crea, edita y elimina mesas.
import { api, norm } from '../api.js';
import { h, vaciar, badge, intentar, vacio } from '../ui.js';
import { abrirFormulario, eliminarConConfirmacion } from './crud.js';

const ESTADOS = ['LIBRE', 'RESERVADA', 'OCUPADA'];
const CAMPOS = [
  { nombre: 'numero', etiqueta: 'Número de mesa', tipo: 'number', requerido: true },
  { nombre: 'capacidad', etiqueta: 'Capacidad (personas)', tipo: 'number', requerido: true },
  { nombre: 'estado', etiqueta: 'Estado', tipo: 'select', valorInicial: 'LIBRE',
    opciones: async () => ESTADOS.map((e) => ({ valor: e, texto: e[0] + e.slice(1).toLowerCase() })) },
];

export async function vistaMesas(cont, ctx) {
  const esAdmin = ctx.tipo === 'ADMINISTRADOR';
  const lista = h('div', { class: 'grid', style: 'grid-template-columns:repeat(auto-fill,minmax(170px,1fr))' });

  const cargar = async () => {
    const mesas = (await api.get('/api/mesas')).sort((a, b) => a.numero - b.numero);
    vaciar(lista);
    if (!mesas.length) { lista.append(vacio('No hay mesas registradas.')); return; }
    lista.append(...mesas.map((m) => h('div', { class: 'card mesa-tile stack', style: 'gap:6px' },
      h('div', { class: 'n' }, m.numero),
      h('div', { class: 'small muted' }, `Capacidad ${m.capacidad}`),
      h('div', {}, badge(m.estado)),
      esAdmin ? [
        h('select', { 'aria-label': 'Estado de la mesa ' + m.numero, onchange: async (e) => {
          await intentar(() => api.patch(`/api/mesas/${m.idMesa}/estado`, { estado: e.target.value }), 'Mesa actualizada');
          await cargar();
        } }, ESTADOS.map((s) => h('option', { value: s, selected: norm(m.estado) === s }, s[0] + s.slice(1).toLowerCase()))),
        h('div', { class: 'row', style: 'justify-content:center' },
          h('button', { class: 'btn sm ghost', type: 'button', onclick: () => abrirFormulario({ titulo: 'mesa', campos: CAMPOS, fila: m, endpoint: '/api/mesas', idKey: 'idMesa', alGuardar: cargar }) }, 'Editar'),
          h('button', { class: 'btn sm ghost', type: 'button', onclick: () => eliminarConConfirmacion('/api/mesas', m.idMesa, `la mesa ${m.numero}`, cargar) }, 'Eliminar')),
      ] : null)));
  };

  cont.append(
    h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, 'Mesas'), h('p', { class: 'muted' }, 'Estado actual del salón.')),
      esAdmin ? h('button', { class: 'btn primary', type: 'button', onclick: () => abrirFormulario({ titulo: 'mesa', campos: CAMPOS, endpoint: '/api/mesas', idKey: 'idMesa', alGuardar: cargar }) }, '+ Nueva mesa') : null),
    lista);
  await cargar();
  const t = setInterval(() => cargar().catch(() => {}), 20000);
  return () => clearInterval(t);
}
