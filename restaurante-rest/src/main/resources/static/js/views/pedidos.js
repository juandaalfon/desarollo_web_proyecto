// Pedidos: listado (todos los roles) y detalle con productos, observaciones y acciones del flujo.
import { api, norm } from '../api.js';
import { h, vaciar, badge, dinero, fechaHora, hora, duracion, modal, confirmar, campo, toast, intentar, vacio } from '../ui.js';

const ACTIVOS = ['PENDIENTE', 'EN_PREPARACION', 'LISTO'];
const FILTROS = [
  ['activos', 'Activos'], ['PENDIENTE', 'Pendientes'], ['EN_PREPARACION', 'En preparación'],
  ['LISTO', 'Listos'], ['ENTREGADO', 'Entregados'], ['CANCELADO', 'Cancelados'], ['todos', 'Todos'],
];

// ======================= Listado =======================
export async function vistaPedidos(cont, ctx) {
  let filtro = 'activos';
  let soloMios = ctx.tipo === 'MESERO';
  let pedidos = [];

  const chips = h('div', { class: 'chips' });
  const lista = h('div', { class: 'grid' });

  const pintar = () => {
    vaciar(chips).append(...FILTROS.map(([k, t]) =>
      h('button', { class: 'chip' + (filtro === k ? ' on' : ''), type: 'button', onclick: () => { filtro = k; pintar(); } }, t)));
    if (ctx.tipo === 'MESERO') {
      chips.append(h('button', { class: 'chip' + (soloMios ? ' on' : ''), type: 'button',
        onclick: () => { soloMios = !soloMios; pintar(); } }, soloMios ? '✓ Solo míos' : 'Solo míos'));
    }
    const visibles = pedidos.filter((p) => {
      const e = norm(p.estado);
      if (filtro === 'activos' ? !ACTIVOS.includes(e) : (filtro !== 'todos' && e !== filtro)) return false;
      return !(soloMios && p.mesero?.idUsuario !== ctx.usuario.idUsuario);
    });
    vaciar(lista);
    if (!visibles.length) { lista.append(vacio('No hay pedidos con este filtro.')); return; }
    lista.append(...visibles.map(tarjeta));
  };

  const cargar = async () => { pedidos = await api.get('/api/pedidos'); pintar(); };

  cont.append(
    h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, 'Pedidos'), h('p', { class: 'muted' }, 'Del más reciente al más antiguo.')),
      ctx.tipo === 'MESERO' ? h('button', { class: 'btn primary', type: 'button', onclick: () => nuevoPedido(ctx) }, '+ Nuevo pedido') : null),
    h('div', { style: 'margin-bottom:16px' }, chips), lista);
  await cargar();
  const t = setInterval(() => cargar().catch(() => {}), 20000);
  return () => clearInterval(t);
}

function tarjeta(p) {
  return h('a', { class: 'card pedido-card', href: '#/pedidos/' + p.idPedido },
    h('div', { class: 'row between' }, h('div', { class: 'mesa' }, 'Mesa ' + (p.mesa?.numero ?? '?')), badge(p.estado)),
    h('div', { class: 'muted small' }, `Pedido #${p.idPedido} · ${p.numPersonas ?? '?'} persona(s)`),
    h('div', { class: 'row between small' },
      h('span', {}, p.mesero?.nombre ?? '—'),
      h('span', { class: 'muted' }, fechaHora(p.fechaCreacion))));
}

// ======================= Nuevo pedido (RF-02) =======================
async function nuevoPedido() {
  const mesas = (await api.get('/api/mesas')).filter((m) => norm(m.estado) === 'LIBRE').sort((a, b) => a.numero - b.numero);
  if (!mesas.length) { toast('No hay mesas libres en este momento.', 'error'); return; }
  const selMesa = h('select', {}, mesas.map((m) => h('option', { value: m.idMesa }, `Mesa ${m.numero} (capacidad ${m.capacidad})`)));
  const personas = h('input', { type: 'number', min: 1, value: 2 });
  modal('Nuevo pedido', h('div', { class: 'stack' }, campo('Mesa libre', selMesa), campo('Número de personas', personas)), [{
    texto: 'Crear pedido', clase: 'primary',
    onclick: async () => {
      let creado;
      const ok = await intentar(async () => {
        creado = await api.post('/api/pedidos', { mesa: { idMesa: Number(selMesa.value) }, numPersonas: Number(personas.value) });
      }, 'Pedido creado');
      if (!ok) return false;
      location.hash = '#/pedidos/' + creado.idPedido;
    },
  }]);
}

// ======================= Detalle =======================
export async function vistaPedidoDetalle(cont, ctx) {
  const id = Number(ctx.id);
  const cuerpo = h('div', {});
  cont.append(h('p', {}, h('a', { href: '#/pedidos' }, '← Volver a pedidos')), cuerpo);
  let vivo = true;

  const cargar = async () => {
    const [pedido, detalles, historial] = await Promise.all([
      api.get('/api/pedidos/' + id), api.get('/api/pedidos/' + id + '/detalles'), api.get('/api/pedidos/' + id + '/historial'),
    ]);
    if (vivo) pintar(pedido, detalles, historial);
  };

  const pintar = (pedido, detalles, historial) => {
    const estado = norm(pedido.estado);
    const activo = estado === 'PENDIENTE' || estado === 'EN_PREPARACION';
    const esAdmin = ctx.tipo === 'ADMINISTRADOR';
    const esDuenio = ctx.tipo === 'MESERO' && pedido.mesero?.idUsuario === ctx.usuario.idUsuario;
    const puedeEditar = (esAdmin || esDuenio) && activo;
    const total = detalles.reduce((s, d) => s + Number(d.subtotal ?? 0), 0);
    const recargar = () => cargar().catch((e) => toast(e.message, 'error'));

    const accion = (texto, clase, fn, ok) => h('button', { class: 'btn ' + clase, type: 'button',
      onclick: async () => { if (await intentar(fn, ok)) recargar(); } }, texto);
    const cambiar = (nuevo) => () => api.patch(`/api/pedidos/${id}/estado`, { estado: nuevo });

    const acciones = [];
    if ((ctx.tipo === 'COCINA' || esAdmin) && estado === 'PENDIENTE') acciones.push(accion('▶ Iniciar preparación', 'primary', cambiar('EN_PREPARACION'), 'En preparación'));
    if ((ctx.tipo === 'COCINA' || esAdmin) && estado === 'EN_PREPARACION') acciones.push(accion('✓ Marcar listo', 'ok', cambiar('LISTO'), 'Pedido listo'));
    if ((esAdmin || esDuenio) && estado === 'LISTO') acciones.push(accion('Entregar a la mesa', 'ok', () => api.patch(`/api/pedidos/${id}/entregar`), 'Pedido entregado'));
    if (puedeEditar) acciones.push(h('button', { class: 'btn', type: 'button', onclick: () => cambiarMesa(pedido, recargar) }, 'Cambiar mesa'));
    if (puedeEditar) acciones.push(h('button', { class: 'btn danger', type: 'button', onclick: async () => {
      if (await confirmar('Cancelar pedido', `¿Cancelar el pedido #${id}? La mesa quedará libre.`, 'Sí, cancelar', 'danger')) {
        if (await intentar(() => api.patch(`/api/pedidos/${id}/cancelar`), 'Pedido cancelado')) recargar();
      } } }, 'Cancelar pedido'));
    if (esAdmin && estado === 'ENTREGADO') acciones.push(h('button', { class: 'btn primary', type: 'button', onclick: () => facturar(pedido, total, ctx, recargar) }, 'Generar factura'));
    if (esAdmin && (estado === 'PENDIENTE' || estado === 'CANCELADO')) acciones.push(h('button', { class: 'btn ghost', type: 'button', onclick: async () => {
      if (await confirmar('Eliminar pedido', 'Se borrará el pedido con sus productos e historial.', 'Eliminar', 'danger')) {
        if (await intentar(() => api.del('/api/pedidos/' + id), 'Pedido eliminado')) location.hash = '#/pedidos';
      } } }, 'Eliminar'));

    const filas = detalles.map((d) => h('tr', {},
      h('td', {}, h('div', {}, d.producto?.nombre), d.notas ? h('div', { class: 'note' }, '📝 ' + d.notas) : null),
      h('td', { class: 'num' }, puedeEditar
        ? h('span', { class: 'qty' },
          h('button', { class: 'btn sm', type: 'button', 'aria-label': 'Menos', disabled: d.cantidad <= 1,
            onclick: async () => { if (await intentar(() => api.patch('/api/detalles-pedido/' + d.idDetalle, { cantidad: d.cantidad - 1 }))) recargar(); } }, '−'),
          h('b', {}, d.cantidad),
          h('button', { class: 'btn sm', type: 'button', 'aria-label': 'Más',
            onclick: async () => { if (await intentar(() => api.patch('/api/detalles-pedido/' + d.idDetalle, { cantidad: d.cantidad + 1 }))) recargar(); } }, '+'))
        : d.cantidad),
      h('td', { class: 'num' }, dinero(d.precioUnitario)),
      h('td', { class: 'num' }, dinero(d.subtotal)),
      h('td', { class: 'actions' }, puedeEditar ? [
        h('button', { class: 'btn sm ghost', type: 'button', onclick: () => editarNota(d, recargar) }, 'Nota'),
        h('button', { class: 'btn sm ghost', type: 'button', 'aria-label': 'Quitar', onclick: async () => {
          if (await intentar(() => api.del('/api/detalles-pedido/' + d.idDetalle), 'Producto quitado')) recargar(); } }, '✕'),
      ] : null)));

    vaciar(cuerpo).append(
      h('div', { class: 'page-head' },
        h('div', {}, h('h1', {}, `Mesa ${pedido.mesa?.numero ?? '?'} · Pedido #${id}`),
          h('p', { class: 'muted' }, `${pedido.numPersonas ?? '?'} persona(s) · ${pedido.mesero?.nombre ?? '—'} · creado ${fechaHora(pedido.fechaCreacion)}`)),
        badge(pedido.estado)),
      acciones.length ? h('div', { class: 'row', style: 'margin-bottom:16px' }, acciones) : null,
      h('div', { class: 'detail' },
        h('div', { class: 'stack' },
          h('div', { class: 'card' },
            h('div', { class: 'row between', style: 'margin-bottom:10px' }, h('h2', {}, 'Productos'),
              puedeEditar ? h('button', { class: 'btn primary sm', type: 'button', onclick: () => agregarProducto(id, recargar) }, '+ Agregar producto') : null),
            detalles.length
              ? h('div', { class: 'table-wrap', style: 'border:0;border-radius:0' }, h('table', {},
                h('thead', {}, h('tr', {}, h('th', {}, 'Producto'), h('th', { class: 'num' }, 'Cant.'), h('th', { class: 'num' }, 'Precio'), h('th', { class: 'num' }, 'Subtotal'), h('th', {}))),
                h('tbody', {}, filas),
                h('tfoot', {}, h('tr', {}, h('td', { colspan: 3, class: 'right' }, h('b', {}, 'Total')), h('td', { class: 'num' }, h('b', {}, dinero(total))), h('td', {})))))
              : vacio('Aún no hay productos en este pedido.'))),
        h('div', { class: 'card' }, h('h2', { style: 'margin-bottom:12px' }, 'Historial'), lineaTiempo(historial))));
  };

  await cargar();
  const t = setInterval(() => cargar().catch(() => {}), 15000);
  return () => { vivo = false; clearInterval(t); };
}

function lineaTiempo(hist) {
  if (!hist?.etapas?.length) return vacio('Sin historial.');
  return h('div', {},
    h('ul', { class: 'timeline' }, hist.etapas.map((e) => h('li', {},
      h('div', { class: 'row between' }, badge(e.estado), h('span', { class: 'small muted' }, hora(e.fechaHora))),
      h('div', { class: 'small muted' }, `${e.usuario ?? ''}${e.duracionSegundos != null ? ' · duró ' + duracion(e.duracionSegundos) : ' · estado actual'}`)))),
    h('p', { class: 'small muted', style: 'margin-top:14px' }, 'Tiempo total: ' + duracion(hist.tiempoTotalSegundos) + (hist.finalizado ? '' : ' (en curso)')));
}

// ---------- Diálogos del detalle ----------
async function agregarProducto(idPedido, recargar) {
  const productos = (await api.get('/api/productos')).filter((p) => p.disponible);
  if (!productos.length) { toast('No hay productos disponibles.', 'error'); return; }
  const porCategoria = new Map();
  productos.forEach((p) => {
    const c = p.categoria?.nombre ?? 'Otros';
    if (!porCategoria.has(c)) porCategoria.set(c, []);
    porCategoria.get(c).push(p);
  });
  let elegido = null;
  const info = h('div', { class: 'small muted' }, 'Elige un producto');
  const cantidad = h('input', { type: 'number', min: 1, value: 1 });
  const notas = h('input', { type: 'text', maxLength: 255, placeholder: 'Ej.: sin cebolla' });
  const items = [];
  const lista = h('div', {}, [...porCategoria].map(([cat, ps]) => h('div', { class: 'menu-cat' },
    h('h3', {}, cat), ps.map((p) => {
      const fila = h('button', { class: 'menu-item btn ghost', type: 'button', style: 'width:100%;justify-content:space-between;border-radius:0',
        onclick: () => { elegido = p; items.forEach((i) => i.style.background = ''); fila.style.background = 'var(--brand-soft)'; info.textContent = `${p.nombre} · ${dinero(p.precio)}`; } },
        h('span', {}, p.nombre), h('span', { class: 'muted' }, dinero(p.precio)));
      items.push(fila);
      return fila;
    }))));
  modal('Agregar producto', h('div', { class: 'stack' }, h('div', { style: 'max-height:38vh;overflow:auto' }, lista), info,
    h('div', { class: 'row' }, h('div', { style: 'width:100px' }, campo('Cantidad', cantidad)), h('div', { class: 'grow' }, campo('Observación (opcional)', notas)))),
  [{ texto: 'Agregar', clase: 'primary', onclick: async () => {
    if (!elegido) { toast('Elige un producto.', 'error'); return false; }
    const ok = await intentar(() => api.post('/api/detalles-pedido', {
      pedido: { idPedido }, producto: { idProducto: elegido.idProducto }, cantidad: Number(cantidad.value),
      ...(notas.value.trim() ? { notas: notas.value.trim() } : {}),
    }), 'Producto agregado');
    if (!ok) return false;
    recargar();
  } }]);
}

function editarNota(d, recargar) {
  const nota = h('textarea', { maxLength: 255 }, d.notas ?? '');
  modal(`Observación · ${d.producto?.nombre}`, campo('Máx. 255 caracteres. Déjalo vacío para borrarla.', nota), [{
    texto: 'Guardar', clase: 'primary', onclick: async () => {
      const ok = await intentar(() => api.patch(`/api/detalles-pedido/${d.idDetalle}/notas`, { notas: nota.value.trim() }), 'Observación guardada');
      if (!ok) return false;
      recargar();
    } }]);
}

async function cambiarMesa(pedido, recargar) {
  const mesas = (await api.get('/api/mesas')).filter((m) => norm(m.estado) === 'LIBRE').sort((a, b) => a.numero - b.numero);
  if (!mesas.length) { toast('No hay mesas libres.', 'error'); return; }
  const sel = h('select', {}, mesas.map((m) => h('option', { value: m.idMesa }, `Mesa ${m.numero} (capacidad ${m.capacidad})`)));
  modal('Cambiar de mesa', campo(`Actual: mesa ${pedido.mesa?.numero}. Nueva mesa:`, sel), [{
    texto: 'Cambiar', clase: 'primary', onclick: async () => {
      const ok = await intentar(() => api.put(`/api/pedidos/${pedido.idPedido}/mesa`, { idMesa: Number(sel.value) }), 'Mesa actualizada');
      if (!ok) return false;
      recargar();
    } }]);
}

function facturar(pedido, total, ctx, recargar) {
  modal('Generar factura', h('p', {}, `Pedido #${pedido.idPedido} · Mesa ${pedido.mesa?.numero} · Total ${dinero(total)}`), [{
    texto: 'Generar', clase: 'primary', onclick: async () => {
      const ahora = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 19);
      const ok = await intentar(() => api.post('/api/facturas', {
        pedido: { idPedido: pedido.idPedido }, fechaGeneracion: ahora, total, usuarioGenera: { idUsuario: ctx.usuario.idUsuario },
      }), 'Factura generada');
      if (!ok) return false;
      recargar();
    } }]);
}

