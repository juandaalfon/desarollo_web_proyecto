// Utilidades de interfaz: creación de nodos, formato, avisos y diálogos.
import { norm } from './api.js';

/** h('div', {class:'x', onclick: fn}, hijo1, 'texto', ...) — siempre usa textContent, nunca innerHTML. */
export function h(tag, attrs = {}, ...hijos) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === false || v == null) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'value') el.value = v;
    else if (k in el && k !== 'list' && k !== 'form') el[k] = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  agregar(el, hijos);
  return el;
}
function agregar(el, hijos) {
  for (const hijo of hijos.flat(Infinity)) {
    if (hijo == null || hijo === false) continue;
    el.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
  }
}
export const vaciar = (el) => { el.replaceChildren(); return el; };

// ---------- Formato ----------
export const dinero = (n) => '$ ' + Number(n ?? 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });

export function fechaHora(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}
export const hora = (iso) => (iso ? new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }) : '—');

export function duracion(seg) {
  if (seg == null) return '—';
  seg = Math.round(seg);
  if (seg < 60) return `${seg} s`;
  const m = Math.floor(seg / 60);
  if (m < 60) return `${m} min ${seg % 60 ? (seg % 60) + ' s' : ''}`.trim();
  return `${Math.floor(m / 60)} h ${m % 60} min`;
}
export const minutosDesde = (iso) => (iso ? Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 60000)) : 0);

const ETIQUETAS = {
  PENDIENTE: 'Pendiente', EN_PREPARACION: 'En preparación', LISTO: 'Listo', ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado', LIBRE: 'Libre', RESERVADA: 'Reservada', OCUPADA: 'Ocupada',
};
export const etiqueta = (estado) => ETIQUETAS[norm(estado)] ?? estado ?? '—';
export const badge = (estado) => h('span', { class: 'badge b-' + norm(estado) }, etiqueta(estado));
export const si = (v) => h('span', { class: 'badge ' + (v ? 'b-ON' : 'b-OFF') }, v ? 'Sí' : 'No');

// ---------- Avisos ----------
export function toast(mensaje, tipo = 'ok') {
  const t = h('div', { class: 'toast ' + (tipo === 'error' ? 'error' : ''), role: tipo === 'error' ? 'alert' : 'status' }, mensaje);
  document.getElementById('toasts').append(t);
  setTimeout(() => t.remove(), tipo === 'error' ? 6000 : 3000);
}

/** Ejecuta una acción async mostrando el error del servidor como aviso. Devuelve true si salió bien. */
export async function intentar(fn, mensajeOk) {
  try {
    await fn();
    if (mensajeOk) toast(mensajeOk);
    return true;
  } catch (e) {
    toast(e.message || 'Ocurrió un error.', 'error');
    return false;
  }
}

// ---------- Diálogos ----------
/** Abre un modal. `cuerpo` es un nodo; `acciones` = [{texto, clase, onclick: async()=>cerrar?}] */
export function modal(titulo, cuerpo, acciones = [], alCerrar) {
  const anterior = document.activeElement;
  const cerrar = () => { overlay.remove(); document.removeEventListener('keydown', teclas); anterior?.focus?.(); alCerrar?.(); };
  const teclas = (e) => { if (e.key === 'Escape') cerrar(); };

  const botones = acciones.map((a) => {
    const b = h('button', { class: 'btn ' + (a.clase || ''), type: 'button' }, a.texto);
    b.addEventListener('click', async () => {
      b.disabled = true;
      try {
        const cierra = await a.onclick?.();
        if (cierra !== false) cerrar();
      } finally { b.disabled = false; }
    });
    return b;
  });
  const dialogo = h('div', { class: 'modal', role: 'dialog', 'aria-modal': 'true', 'aria-label': titulo },
    h('header', {}, h('h2', {}, titulo)),
    h('div', { class: 'body' }, cuerpo),
    h('footer', {},
      h('button', { class: 'btn', type: 'button', onclick: cerrar }, 'Cancelar'),
      ...botones));
  const overlay = h('div', { class: 'overlay', onmousedown: (e) => { if (e.target === overlay) cerrar(); } }, dialogo);
  document.body.append(overlay);
  document.addEventListener('keydown', teclas);
  dialogo.querySelector('input, select, textarea')?.focus();
  return cerrar;
}

export function confirmar(titulo, mensaje, textoOk = 'Confirmar', clase = 'primary') {
  return new Promise((resolver) => {
    let decidido = false;
    modal(titulo, h('p', {}, mensaje),
      [{ texto: textoOk, clase, onclick: () => { decidido = true; resolver(true); } }],
      () => { if (!decidido) resolver(false); });
  });
}

export const campo = (etiquetaTxt, control) => h('label', { class: 'field' }, etiquetaTxt, control);
export const vacio = (txt) => h('div', { class: 'empty' }, txt);
export const cargando = () => h('div', { class: 'spinner' }, 'Cargando…');
