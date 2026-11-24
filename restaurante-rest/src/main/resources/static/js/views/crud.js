// Tabla + formulario genéricos para los CRUD del administrador (usa los 6 verbos de /api/{recurso}).
import { api } from '../api.js';
import { h, vaciar, modal, campo, intentar, confirmar, vacio, cargando } from '../ui.js';

/**
 * Campo: {nombre, etiqueta, tipo: text|number|textarea|select|checkbox|password, requerido, fk, opciones: async()=>[{valor,texto}],
 *         valorInicial, soloCrear, ayuda}
 * Las llaves foráneas (fk) se envían como objeto: {categoria: {idCategoria: 3}}.
 */
export async function abrirFormulario({ titulo, campos, fila, endpoint, idKey, alGuardar }) {
  const editando = !!fila;
  const controles = new Map();
  const nodos = [];
  for (const c of campos) {
    let ctl;
    const actual = fila ? (c.fk ? fila[c.nombre]?.[c.fk] : fila[c.nombre]) : c.valorInicial;
    if (c.tipo === 'select') {
      const ops = await c.opciones();
      ctl = h('select', {}, ops.map((o) => h('option', { value: o.valor, selected: String(o.valor) === String(actual) }, o.texto)));
    } else if (c.tipo === 'checkbox') {
      ctl = h('input', { type: 'checkbox', checked: actual ?? true });
      nodos.push(h('label', { class: 'check' }, ctl, c.etiqueta));
      controles.set(c, ctl);
      continue;
    } else if (c.tipo === 'textarea') {
      ctl = h('textarea', {}, actual ?? '');
    } else {
      ctl = h('input', { type: c.tipo || 'text', value: c.tipo === 'password' ? '' : (actual ?? ''), step: c.tipo === 'number' ? 'any' : null,
        autocomplete: c.tipo === 'password' ? 'new-password' : 'off' });
    }
    const etiqueta = c.etiqueta + (c.requerido && !(c.tipo === 'password' && editando) ? ' *' : '');
    nodos.push(campo(etiqueta, ctl));
    if (c.ayuda || (c.tipo === 'password' && editando)) nodos.push(h('div', { class: 'small muted', style: 'margin-top:-8px' }, c.ayuda || 'Déjala vacía para no cambiarla.'));
    controles.set(c, ctl);
  }

  modal((editando ? 'Editar ' : 'Nuevo ') + titulo, h('div', { class: 'stack' }, nodos), [{
    texto: 'Guardar', clase: 'primary',
    onclick: async () => {
      const cuerpo = {};
      for (const [c, ctl] of controles) {
        let v = c.tipo === 'checkbox' ? ctl.checked : ctl.value.trim?.() ?? ctl.value;
        if (c.tipo === 'password' && editando && !v) continue;
        if (c.soloCrear && editando) continue;
        if (c.requerido && (v === '' || v == null)) { await intentar(async () => { throw new Error(`${c.etiqueta} es obligatorio.`); }); return false; }
        if (c.tipo === 'number') v = v === '' ? null : Number(v);
        if (c.tipo === 'select' && c.fk) v = { [c.fk]: Number(v) };
        else if (c.tipo === 'select' && c.numero) v = Number(v);
        if (v === '' && !c.requerido) v = null;
        cuerpo[c.nombre] = v;
      }
      const ok = await intentar(() => (editando ? api.patch(`${endpoint}/${fila[idKey]}`, cuerpo) : api.post(endpoint, cuerpo)),
        editando ? 'Cambios guardados' : 'Creado correctamente');
      if (!ok) return false;
      await alGuardar?.();
    },
  }]);
}

export async function eliminarConConfirmacion(endpoint, id, descripcion, alEliminar) {
  if (!(await confirmar('Eliminar', `¿Eliminar ${descripcion}? Esta acción no se puede deshacer.`, 'Eliminar', 'danger'))) return;
  if (await intentar(() => api.del(`${endpoint}/${id}`), 'Eliminado')) await alEliminar?.();
}

/** cfg: {titulo, singular, endpoint, idKey, columnas:[{texto, num, celda(fila)}], campos, orden(a,b), descripcion(fila), acciones(fila, recargar)} */
export async function tablaCrud(cont, cfg) {
  const caja = h('div', {}, cargando());
  const recargar = async () => {
    const filas = await api.get(cfg.endpoint);
    if (cfg.orden) filas.sort(cfg.orden);
    vaciar(caja);
    if (!filas.length) { caja.append(vacio(`Aún no hay ${cfg.titulo.toLowerCase()}.`)); return; }
    caja.append(h('div', { class: 'table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {}, cfg.columnas.map((c) => h('th', { class: c.num ? 'num' : '' }, c.texto)), h('th', {}))),
      h('tbody', {}, filas.map((f) => h('tr', {},
        cfg.columnas.map((c) => h('td', { class: c.num ? 'num' : '' }, c.celda(f))),
        h('td', { class: 'actions' },
          cfg.acciones?.(f, recargar),
          h('button', { class: 'btn sm', type: 'button', onclick: () => abrirFormulario({ titulo: cfg.singular, campos: cfg.campos, fila: f, endpoint: cfg.endpoint, idKey: cfg.idKey, alGuardar: recargar }) }, 'Editar'), ' ',
          h('button', { class: 'btn sm danger', type: 'button', onclick: () => eliminarConConfirmacion(cfg.endpoint, f[cfg.idKey], cfg.descripcion(f), recargar) }, 'Eliminar'))))))));
  };
  cont.append(
    h('div', { class: 'page-head' }, h('h2', {}, cfg.titulo),
      cfg.sinCrear ? null : h('button', { class: 'btn primary', type: 'button', onclick: () => abrirFormulario({ titulo: cfg.singular, campos: cfg.campos, endpoint: cfg.endpoint, idKey: cfg.idKey, alGuardar: recargar }) }, '+ Nuevo')),
    caja);
  await recargar();
}
