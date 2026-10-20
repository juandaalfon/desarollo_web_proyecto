package api.service;

import api.exception.OperacionInvalidaException;

/** Validaciones de texto reutilizables por los servicios. */
final class Validar {

    private Validar() {
    }

    /** Texto obligatorio: no vacío y con máximo de caracteres. Devuelve el texto sin espacios sobrantes. */
    static String texto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new OperacionInvalidaException("El campo '" + campo + "' es obligatorio.");
        }
        String t = valor.trim();
        if (t.length() > max) {
            throw new OperacionInvalidaException("El campo '" + campo + "' no puede superar " + max + " caracteres.");
        }
        return t;
    }

    /** Texto opcional: devuelve null si viene vacío. */
    static String textoOpcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return texto(valor, campo, max);
    }
}
