CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    apellido VARCHAR(45) NOT NULL,
    cedula INT NOT NULL,
    correo VARCHAR(45) NOT NULL UNIQUE,
    telefono BIGINT,          -- BIGINT en vez de INT: un celular de 10 dígitos no cabe en INT
    `contraseña` VARCHAR(45) NOT NULL
);
