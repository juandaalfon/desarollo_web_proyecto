// Punto de entrada: login, barra de navegación por rol y enrutador por hash (#/ruta).
import { api, sesion, onSesionExpirada } from './api.js';
import { h, vaciar, toast, cargando } from './ui.js';
import { vistaPedidos, vistaPedidoDetalle } from './views/pedidos.js';
import { vistaCocina } from './views/cocina.js';
import { vistaMesas } from './views/mesas.js';
import { vistaResumen } from './views/resumen.js';
import { vistaMenu } from './views/menu.js';
import { vistaUsuarios } from './views/usuarios.js';
import { vistaFacturas } from './views/facturas.js';

const raiz = document.getElementById('app');
let limpiar = null; // función que detiene temporizadores de la vista actual

/** Rol del sistema a partir del nombre del rol en la BD (misma regla que TipoRol.java). */
export function tipoRol(nombreRol) {
  const n = (nombreRol || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase();
  if (n.includes('ADMIN')) return 'ADMINISTRADOR';
  if (n.includes('MESER')) return 'MESERO';
  if (n.includes('COCIN')) return 'COCINA';
  return null;
}

const RUTAS = {
  MESERO: [
    { ruta: 'pedidos', texto: 'Pedidos', vista: vistaPedidos },
    { ruta: 'mesas', texto: 'Mesas', vista: vistaMesas },
  ],
  COCINA: [
    { ruta: 'cocina', texto: 'Cocina', vista: vistaCocina },
    { ruta: 'pedidos', texto: 'Todos los pedidos', vista: vistaPedidos },
  ],
  ADMINISTRADOR: [
    { ruta: 'resumen', texto: 'Resumen', vista: vistaResumen },
    { ruta: 'pedidos', texto: 'Pedidos', vista: vistaPedidos },
    { ruta: 'cocina', texto: 'Cocina', vista: vistaCocina },
    { ruta: 'menu', texto: 'Menú', vista: vistaMenu },
    { ruta: 'mesas', texto: 'Mesas', vista: vistaMesas },
    { ruta: 'usuarios', texto: 'Usuarios', vista: vistaUsuarios },
    { ruta: 'facturas', texto: 'Facturas', vista: vistaFacturas },
  ],
};

function contexto() {
  const s = sesion.get();
  return { usuario: s.usuario, tipo: tipoRol(s.usuario.rol) };
}

// ---------- Login (RF-01) ----------
function pintarLogin() {
  limpiar?.(); limpiar = null;
  const error = h('div', { class: 'err', hidden: true, role: 'alert' });
  const usuario = h('input', { type: 'text', autocomplete: 'username', required: true, autofocus: true });
  const clave = h('input', { type: 'password', autocomplete: 'current-password', required: true });
  const boton = h('button', { class: 'btn primary lg', type: 'submit' }, 'Entrar');

  const form = h('form', { class: 'login-card', onsubmit: async (e) => {
    e.preventDefault();
    error.hidden = true; boton.disabled = true; boton.textContent = 'Entrando…';
    try {
      const r = await api.post('/api/auth/login', { usuarioLogin: usuario.value.trim(), contrasena: clave.value });
      sesion.set({ token: r.token, expira: r.expira, usuario: r.usuario });
      location.hash = '';
      iniciar();
    } catch (err) {
      error.textContent = err.message; error.hidden = false;
    } finally {
      boton.disabled = false; boton.textContent = 'Entrar';
    }
  } },
    h('div', {}, h('h2', {}, 'Iniciar sesión'), h('p', { class: 'muted' }, 'Usa tu usuario y contraseña del restaurante.')),
    error,
    h('label', { class: 'field' }, 'Usuario', usuario),
    h('label', { class: 'field' }, 'Contraseña', clave),
    boton,
    h('div', { class: 'demo' },
      h('div', { class: 'small muted' }, 'Usuarios de prueba (datos iniciales de la Semilla)'),
      h('div', { class: 'chips' },
        ...[['admin', 'admin123', 'Administrador'], ['mesero', 'mesero123', 'Mesero'], ['cocina', 'cocina123', 'Cocina']]
          .map(([u, c, r]) => h('button', { class: 'chip', type: 'button', onclick: () => { usuario.value = u; clave.value = c; clave.focus(); } }, r)))));

  vaciar(raiz).append(h('div', { class: 'login' },
    h('section', { class: 'login-hero' },
      h('div', { class: 'brand', style: 'color:#fff' }, 'Restaurante'),
      h('div', {}, h('h1', {}, 'Del pedido a la mesa, sin perder el hilo.'),
        h('p', {}, 'Toma de pedidos, cola de cocina, menú, mesas y reportes en un solo lugar.')),
      h('div', { class: 'badge-row' }, ['Mesero', 'Cocina', 'Administrador'].map((t) => h('span', { class: 'pill' }, t)))),
    h('section', { class: 'login-form' }, form)));
}

// ---------- Aplicación ----------
function cerrarSesion(aviso) {
  sesion.clear();
  limpiar?.(); limpiar = null;
  pintarLogin();
  if (aviso) toast(aviso, 'error');
}

async function salir() {
  try { await api.post('/api/auth/logout'); } catch { /* ya expiró */ }
  cerrarSesion();
}

function iniciar() {
  const s = sesion.get();
  if (!s?.token) return pintarLogin();
  const { usuario, tipo } = contexto();
  if (!tipo) return cerrarSesion('Tu rol no tiene acceso al sistema.');

  const principal = h('main', { id: 'main' });
  const nav = h('nav', { class: 'nav', 'aria-label': 'Secciones' },
    RUTAS[tipo].map((r) => h('a', { href: '#/' + r.ruta, 'data-ruta': r.ruta }, r.texto)));
  vaciar(raiz).append(
    h('header', { class: 'topbar' },
      h('div', { class: 'brand' }, 'Restaurante'),
      nav,
      h('div', { class: 'who' },
        h('div', {}, h('div', { class: 'name' }, usuario.nombre), h('div', { class: 'role' }, usuario.rol)),
        h('button', { class: 'btn sm', type: 'button', onclick: salir }, 'Salir'))),
    principal);
  enrutar();
}

async function enrutar() {
  const s = sesion.get();
  if (!s?.token) return;
  const { tipo } = contexto();
  const principal = document.getElementById('main');
  if (!principal) return;

  const [ruta, ...resto] = (location.hash.replace(/^#\/?/, '') || '').split('/');
  const rutas = RUTAS[tipo];
  const actual = rutas.find((r) => r.ruta === (ruta || rutas[0].ruta)) || rutas[0];
  if (!ruta || actual.ruta !== ruta) { location.hash = '#/' + actual.ruta; return; }

  document.querySelectorAll('.nav a').forEach((a) => a.classList.toggle('active', a.dataset.ruta === actual.ruta));
  limpiar?.(); limpiar = null;
  vaciar(principal).append(cargando());
  document.title = actual.texto + ' · Restaurante';

  const ctx = { ...contexto(), id: resto[0] };
  try {
    const contenedor = h('div', {});
    let vista = actual.vista;
    if (actual.ruta === 'pedidos' && ctx.id) vista = vistaPedidoDetalle;
    const resultado = await vista(contenedor, ctx);
    if (typeof resultado === 'function') limpiar = resultado;
    vaciar(principal).append(contenedor);
  } catch (e) {
    vaciar(principal).append(h('div', { class: 'empty' }, e.message));
  }
}

onSesionExpirada(() => cerrarSesion('Tu sesión expiró. Inicia sesión de nuevo.'));
window.addEventListener('hashchange', enrutar);
iniciar();
