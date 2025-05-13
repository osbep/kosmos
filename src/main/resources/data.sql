
-- Precarga de Consultorios
INSERT INTO consultorio (id, numero, piso) VALUES (1, '101', 1);
INSERT INTO consultorio (id, numero, piso) VALUES (2, '102', 1);
INSERT INTO consultorio (id, numero, piso) VALUES (3, '201', 2);

-- Precarga de Doctores
INSERT INTO doctor (id, nombre, apellido_paterno, apellido_materno, especialidad) 
VALUES (1, 'Laura', 'González', 'Méndez', 'Medicina Interna');

INSERT INTO doctor (id, nombre, apellido_paterno, apellido_materno, especialidad) 
VALUES (2, 'Carlos', 'Ramos', 'Sánchez', 'Medicina Interna');

INSERT INTO doctor (id, nombre, apellido_paterno, apellido_materno, especialidad) 
VALUES (3, 'Ana', 'López', 'Fernández', 'Medicina Interna');
