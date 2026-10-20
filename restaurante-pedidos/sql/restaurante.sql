-- Script equivalente al diagrama (sin factura ni menu: no se usan en las 5 funcionalidades).
-- Hibernate crea las tablas solo (hbm2ddl.auto = update); este archivo es solo de referencia o para crearlas a mano.
CREATE DATABASE IF NOT EXISTS restaurante;
USE restaurante;

CREATE TABLE IF NOT EXISTS rol (
    id_rol INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario_login VARCHAR(45) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(100) NOT NULL,
    id_rol INT NOT NULL,
    activo BOOLEAN NOT NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
);

CREATE TABLE IF NOT EXISTS mesa (
    id_mesa INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL,
    capacidad INT NOT NULL,
    estado VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS categoria (
    id_categoria INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS producto (
    id_producto INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(10,2) NOT NULL,
    id_categoria INT NOT NULL,
    disponible BOOLEAN NOT NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
);

CREATE TABLE IF NOT EXISTS pedido (
    id_pedido INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_mesa INT NOT NULL,
    id_mesero INT NOT NULL,
    num_personas INT NOT NULL,
    estado VARCHAR(45) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_ultima_actualizacion DATETIME,
    CONSTRAINT fk_pedido_mesa FOREIGN KEY (id_mesa) REFERENCES mesa (id_mesa),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_mesero) REFERENCES usuario (id_usuario)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id_detalle INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    notas VARCHAR(255),
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_pedido_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_detalle_pedido_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
);

CREATE TABLE IF NOT EXISTS historial_estado (
    id_historial INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    estado VARCHAR(45) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT fk_historial_estado_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_historial_estado_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
