-- =====================================================
-- COLUMNAS ADICIONALES PARA SALES-SERVICE
-- Ejecutar después de init.sql
-- =====================================================

-- Agregar columnas de ganancias a la tabla ventas
ALTER TABLE ventas 
ADD COLUMN IF NOT EXISTS modelo_negocio VARCHAR(20),
ADD COLUMN IF NOT EXISTS ganancia_vendedor DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS parte_samuel DECIMAL(10,2);

-- Agregar columna cantidad_asignada a tandas si no existe
-- (necesaria para sales-service)
ALTER TABLE tandas
ADD COLUMN IF NOT EXISTS cantidad_asignada INTEGER;

-- Actualizar cantidad_asignada para tandas existentes (si hay)
UPDATE tandas 
SET cantidad_asignada = stock_entregado 
WHERE cantidad_asignada IS NULL AND stock_entregado > 0;

-- Comentarios
COMMENT ON COLUMN ventas.modelo_negocio IS 'Modelo aplicado: MODELO_60_40 o MODELO_50_50';
COMMENT ON COLUMN ventas.ganancia_vendedor IS 'Ganancia del vendedor (60% o 50% según modelo)';
COMMENT ON COLUMN ventas.parte_samuel IS 'Parte que sube a Samuel (40% o 50% según modelo)';

-- Índices adicionales para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_ventas_usuario_estado ON ventas(usuario_id, estado);
CREATE INDEX IF NOT EXISTS idx_ventas_tanda ON ventas(tanda_id);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha_registro DESC);
