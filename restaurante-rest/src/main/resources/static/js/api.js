// Cliente HTTP de la API REST. Guarda el token de sesión (RF-01) y lo envía en cada petición.

const KEY = 'restaurante.sesion';

export const sesion = {
  get() {
    try { return JSON.parse(sessionStorage.getItem(KEY)); } catch { return null; }
  },
  set(valor) {
    try { sessionStorage.setItem(KEY, JSON.stringify(valor)); } catch { /* modo privado */ }
  },
  clear() {
    try { sessionStorage.removeItem(KEY); } catch { /* modo privado */ }
  },
};

export class ApiError extends Error {
  constructor(status, mensaje) {
    super(mensaje);
    this.status = status;
  }
}

let alExpirar = () => {};
export function onSesionExpirada(fn) { alExpirar = fn; }

async function pedir(metodo, ruta, cuerpo) {
  const headers = {};
  const s = sesion.get();
  if (s?.token) headers.Authorization = 'Bearer ' + s.token;
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';

  let res;
  try {
    res = await fetch(ruta, { method: metodo, headers, body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor.');
  }
  if (res.status === 204) return null;

  const texto = await res.text();
  let datos = null;
  if (texto) { try { datos = JSON.parse(texto); } catch { datos = texto; } }

  if (!res.ok) {
    if (res.status === 401 && ruta !== '/api/auth/login') alExpirar();
    throw new ApiError(res.status, datos?.error || datos?.message || `Error ${res.status}`);
  }
  return datos;
}

export const api = {
  get: (ruta) => pedir('GET', ruta),
  post: (ruta, cuerpo) => pedir('POST', ruta, cuerpo ?? {}),
  put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo ?? {}),
  patch: (ruta, cuerpo) => pedir('PATCH', ruta, cuerpo),
  del: (ruta) => pedir('DELETE', ruta),
};

/** Normaliza el estado que llega como texto ("en_preparacion " -> "EN_PREPARACION"). */
export const norm = (s) => (s ?? '').toString().trim().toUpperCase();
