CREATE TABLE IF NOT EXISTS producto (
    idproducto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    descripcion VARCHAR(45),
    precio DECIMAL(10,2) NOT NULL,
    disponible TINYINT(1) NOT NULL,
    menu_idmenu INT NOT NULL
);