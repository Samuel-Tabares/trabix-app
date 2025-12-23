-- ============================================
-- TRABIX GRANIZADOS - Inicialización de BD
-- ============================================

-- Extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLAS PRINCIPALES (usando VARCHAR en lugar de ENUM para compatibilidad con JPA)
-- ============================================

-- Usuarios (vendedores, reclutadores, admin)
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'VENDEDOR',
    nivel VARCHAR(10) NOT NULL DEFAULT 'N2',
    reclutador_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    fecha_ingreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuarios_cedula ON usuarios(cedula);
CREATE INDEX idx_usuarios_reclutador ON usuarios(reclutador_id);
CREATE INDEX idx_usuarios_nivel ON usuarios(nivel);
CREATE INDEX idx_usuarios_estado ON usuarios(estado);

-- Lotes (pedidos de granizados)
CREATE TABLE lotes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    cantidad_total INT NOT NULL CHECK (cantidad_total > 0),
    costo_percibido_unitario DECIMAL(10,2) NOT NULL DEFAULT 2400.00,
    modelo VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lotes_usuario ON lotes(usuario_id);
CREATE INDEX idx_lotes_estado ON lotes(estado);

-- Tandas (divisiones de un lote)
CREATE TABLE tandas (
    id BIGSERIAL PRIMARY KEY,
    lote_id BIGINT NOT NULL REFERENCES lotes(id) ON DELETE CASCADE,
    numero INT NOT NULL CHECK (numero BETWEEN 1 AND 3),
    cantidad_asignada INT NOT NULL CHECK (cantidad_asignada > 0),
    stock_entregado INT NOT NULL DEFAULT 0,
    stock_actual INT NOT NULL DEFAULT 0,
    fecha_liberacion TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(lote_id, numero)
);

CREATE INDEX idx_tandas_lote ON tandas(lote_id);
CREATE INDEX idx_tandas_estado ON tandas(estado);

-- Ventas
CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tanda_id BIGINT NOT NULL REFERENCES tandas(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10,2) NOT NULL,
    precio_total DECIMAL(10,2) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_aprobacion TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    nota TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ventas_usuario ON ventas(usuario_id);
CREATE INDEX idx_ventas_tanda ON ventas(tanda_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_ventas_fecha ON ventas(fecha_registro);

-- Cuadres
CREATE TABLE cuadres (
    id BIGSERIAL PRIMARY KEY,
    tanda_id BIGINT NOT NULL REFERENCES tandas(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL,
    monto_esperado DECIMAL(12,2) NOT NULL,
    monto_recibido DECIMAL(12,2) DEFAULT 0,
    excedente DECIMAL(12,2) DEFAULT 0,
    fecha TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    texto_whatsapp TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cuadres_tanda ON cuadres(tanda_id);
CREATE INDEX idx_cuadres_estado ON cuadres(estado);

-- Fondo de Recompensas
CREATE TABLE fondo_recompensas (
    id BIGSERIAL PRIMARY KEY,
    saldo_actual DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Movimientos del Fondo
CREATE TABLE movimientos_fondo (
    id BIGSERIAL PRIMARY KEY,
    fondo_id BIGINT NOT NULL REFERENCES fondo_recompensas(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL,
    monto DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT NOT NULL,
    beneficiario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movimientos_fondo ON movimientos_fondo(fondo_id);
CREATE INDEX idx_movimientos_fecha ON movimientos_fondo(fecha);

-- Configuración de Costos (solo admin)
CREATE TABLE configuracion_costos (
    id BIGSERIAL PRIMARY KEY,
    costo_real_trabix DECIMAL(10,2) NOT NULL DEFAULT 2000.00,
    costo_percibido_trabix DECIMAL(10,2) NOT NULL DEFAULT 2400.00,
    aporte_fondo_por_trabix DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    aporte_gestion_por_trabix DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Costos de Producción (registro de gastos)
CREATE TABLE costos_produccion (
    id BIGSERIAL PRIMARY KEY,
    concepto VARCHAR(100) NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    costo_unitario DECIMAL(10,2) NOT NULL,
    costo_total DECIMAL(12,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_costos_tipo ON costos_produccion(tipo);
CREATE INDEX idx_costos_fecha ON costos_produccion(fecha);

-- Equipos (neveras, pijamas)
CREATE TABLE equipos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    costo_reposicion DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_equipos_usuario ON equipos(usuario_id);
CREATE INDEX idx_equipos_estado ON equipos(estado);

-- Pagos de Mensualidad (equipos)
CREATE TABLE pagos_mensualidad (
    id BIGSERIAL PRIMARY KEY,
    equipo_id BIGINT NOT NULL REFERENCES equipos(id) ON DELETE CASCADE,
    mes INT NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL DEFAULT 10000.00,
    fecha_pago TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(equipo_id, mes, anio)
);

CREATE INDEX idx_pagos_equipo ON pagos_mensualidad(equipo_id);
CREATE INDEX idx_pagos_estado ON pagos_mensualidad(estado);

-- Documentos (cotizaciones, facturas)
CREATE TABLE documentos (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    cliente_nombre VARCHAR(100) NOT NULL,
    cliente_telefono VARCHAR(20),
    cliente_direccion TEXT,
    items JSONB NOT NULL DEFAULT '[]',
    subtotal DECIMAL(12,2) NOT NULL,
    iva DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_vencimiento TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    archivo_pdf_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documentos_tipo ON documentos(tipo);
CREATE INDEX idx_documentos_estado ON documentos(estado);
CREATE INDEX idx_documentos_fecha ON documentos(fecha_emision);

-- Tokens de refresh (para JWT)
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    fecha_expiracion TIMESTAMP NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- ============================================
-- DATOS INICIALES
-- ============================================

-- Configuración de costos inicial
INSERT INTO configuracion_costos (costo_real_trabix, costo_percibido_trabix, aporte_fondo_por_trabix, aporte_gestion_por_trabix)
VALUES (2000.00, 2400.00, 200.00, 200.00);

-- Fondo de recompensas inicial
INSERT INTO fondo_recompensas (saldo_actual) VALUES (0);

-- Usuario admin inicial (password: Guta0214.)
INSERT INTO usuarios (cedula, nombre, telefono, correo, password_hash, rol, nivel, estado)
VALUES (
    '1092456501',
    'Samuel Tabares León',
    '3103034775',
    'trabixgranizados@gmail.com',
    '$2a$10$WjKMrPaDxj7D/i0dH2rYI.XCUUgbfGgLhPj6W82XHLWIYRRES2qca',
    'ADMIN',
    'N1',
    'ACTIVO'
);

-- ============================================
-- FUNCIONES Y TRIGGERS
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_usuarios_updated_at BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lotes_updated_at BEFORE UPDATE ON lotes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tandas_updated_at BEFORE UPDATE ON tandas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ventas_updated_at BEFORE UPDATE ON ventas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cuadres_updated_at BEFORE UPDATE ON cuadres
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fondo_updated_at BEFORE UPDATE ON fondo_recompensas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_config_updated_at BEFORE UPDATE ON configuracion_costos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_equipos_updated_at BEFORE UPDATE ON equipos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pagos_updated_at BEFORE UPDATE ON pagos_mensualidad
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documentos_updated_at BEFORE UPDATE ON documentos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VISTAS ÚTILES
-- ============================================

CREATE VIEW vista_arbol_vendedores AS
WITH RECURSIVE arbol AS (
    SELECT id, nombre, nivel, reclutador_id, 1 as profundidad, ARRAY[id] as path
    FROM usuarios WHERE reclutador_id IS NULL AND rol = 'ADMIN'
    UNION ALL
    SELECT u.id, u.nombre, u.nivel, u.reclutador_id, a.profundidad + 1, a.path || u.id
    FROM usuarios u INNER JOIN arbol a ON u.reclutador_id = a.id
)
SELECT * FROM arbol;

CREATE VIEW vista_resumen_ventas AS
SELECT 
    u.id as usuario_id, u.nombre, u.nivel,
    COUNT(v.id) as total_ventas,
    SUM(CASE WHEN v.estado = 'APROBADA' THEN v.precio_total ELSE 0 END) as total_recaudado,
    SUM(CASE WHEN v.tipo = 'REGALO' THEN v.cantidad ELSE 0 END) as total_regalos
FROM usuarios u LEFT JOIN ventas v ON u.id = v.usuario_id
GROUP BY u.id, u.nombre, u.nivel;
