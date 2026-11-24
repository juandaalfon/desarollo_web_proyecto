// RF-14: usuarios del sistema (la contraseña la cifra el servidor con BCrypt y nunca se devuelve).
import { api } from '../api.js';
import { h, si } from '../ui.js';
import { tablaCrud } from './crud.js';

export async function vistaUsuarios(cont) {
  cont.append(h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Usuarios'), h('p', { class: 'muted' }, 'Administradores, meseros y personal de cocina.'))));
  const caja = h('div', {});
  cont.append(caja);
  await tablaCrud(caja, {
    titulo: 'Usuarios', singular: 'usuario', endpoint: '/api/usuarios', idKey: 'idUsuario',
    orden: (a, b) => a.nombre.localeCompare(b.nombre), descripcion: (u) => `al usuario "${u.usuarioLogin}"`,
    columnas: [
      { texto: 'Nombre', celda: (u) => u.nombre },
      { texto: 'Usuario', celda: (u) => u.usuarioLogin },
      { texto: 'Rol', celda: (u) => u.rol?.nombreRol ?? '—' },
      { texto: 'Activo', celda: (u) => si(u.activo) },
    ],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre completo', requerido: true },
      { nombre: 'usuarioLogin', etiqueta: 'Usuario (login)', requerido: true },
      { nombre: 'contrasenaHash', etiqueta: 'Contraseña', tipo: 'password', requerido: true, ayuda: 'Mínimo 4 caracteres.' },
      { nombre: 'rol', etiqueta: 'Rol', tipo: 'select', fk: 'idRol', requerido: true,
        opciones: async () => (await api.get('/api/roles')).map((r) => ({ valor: r.idRol, texto: r.nombreRol })) },
      { nombre: 'activo', etiqueta: 'Usuario activo', tipo: 'checkbox', valorInicial: true },
    ],
  });
}
