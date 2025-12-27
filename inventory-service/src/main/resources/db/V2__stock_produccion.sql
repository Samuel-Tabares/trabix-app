-- =====================================================
-- TABLAS ADICIONALES PARA INVENTORY-SERVICE
-- Ejecutar después de init.sql
-- =====================================================

-- Tabla para el stock de producción de Samuel (N1)
CREATE TABLE IF NOT EXISTS stock_produccion (
    id BIGSERIAL PRIMARY KEY,
    stock_producido_total INTEGER NOT NULL DEFAULT 0,
    stock_disponible INTEGER NOT NULL DEFAULT 0,
    costo_real_unitario DECIMAL(10,2) DEFAULT 1800.00,
    ultima_produccion TIMESTAMP,
    nivel_alerta_stock_bajo INTEGER NOT NULL DEFAULT 300,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insertar registro inicial si no existe
INSERT INTO stock_produccion (stock_producido_total, stock_disponible, costo_real_unitario, nivel_alerta_stock_bajo)
SELECT 0, 0, 1800.00, 300
WHERE NOT EXISTS (SELECT 1 FROM stock_produccion);

-- Tabla para historial de movimientos de stock
CREATE TABLE IF NOT EXISTS movimientos_stock (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    cantidad INTEGER NOT NULL,
    stock_resultante INTEGER NOT NULL,
    costo_unitario DECIMAL(10,2),
    lote_id BIGINT REFERENCES lotes(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    descripcion VARCHAR(500),
    fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_tipo_movimiento CHECK (tipo IN ('PRODUCCION', 'ENTREGA', 'DEVOLUCION', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'VENTA_DIRECTA'))
);

-- Índices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_stock(tipo);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_stock(fecha_movimiento DESC);
CREATE INDEX IF NOT EXISTS idx_movimientos_lote ON movimientos_stock(lote_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_usuario ON movimientos_stock(usuario_id);

-- Comentarios
COMMENT ON TABLE stock_produccion IS 'Stock de producción de Samuel (N1). Solo debe existir un registro.';
COMMENT ON TABLE movimientos_stock IS 'Historial de movimientos del stock de producción (entradas y salidas).';

COMMENT ON COLUMN stock_produccion.stock_producido_total IS 'Total histórico de TRABIX producidos';
COMMENT ON COLUMN stock_produccion.stock_disponible IS 'TRABIX físicos disponibles en congeladores de Samuel';
COMMENT ON COLUMN stock_produccion.costo_real_unitario IS 'Costo real promedio por TRABIX producido';
COMMENT ON COLUMN stock_produccion.nivel_alerta_stock_bajo IS 'Nivel para generar alerta de stock bajo';

COMMENT ON COLUMN movimientos_stock.tipo IS 'Tipo: PRODUCCION, ENTREGA, DEVOLUCION, AJUSTE_POSITIVO, AJUSTE_NEGATIVO, VENTA_DIRECTA';
COMMENT ON COLUMN movimientos_stock.stock_resultante IS 'Stock disponible después del movimiento';
