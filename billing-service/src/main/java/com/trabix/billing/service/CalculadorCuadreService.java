package com.trabix.billing.service;

import com.trabix.billing.dto.CalculoCuadreResponse;
import com.trabix.billing.entity.*;
import com.trabix.billing.repository.*;
import com.trabix.common.enums.TipoCuadre;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para calcular cuadres según el modelo de negocio TRABIX.
 * 
 * ═══════════════════════════════════════════════════════════════════
 * MODELO DE INVERSIÓN (SIEMPRE IGUAL):
 * - Samuel pone 50%, Vendedor pone 50%
 * - NO depende del nivel ni del modelo
 * ═══════════════════════════════════════════════════════════════════
 * 
 * MODELO DE GANANCIAS (depende del nivel):
 * - MODELO_60_40 (N2): 60% vendedor, 40% Samuel
 * - MODELO_50_50 (N3+): 50% vendedor, 50% sube a Samuel (él distribuye cascada)
 * 
 * ═══════════════════════════════════════════════════════════════════
 * TANDAS (2 o 3 según cantidad):
 * 
 * 2 TANDAS (≤50 TRABIX):
 * - T1: Recuperar inversión Samuel (trigger: recaudado >= inversión Samuel)
 * - T2: Recuperar inversión vendedor + Ganancias (trigger: stock ≤20%)
 * 
 * 3 TANDAS (>50 TRABIX):
 * - T1: Recuperar inversión Samuel (trigger: recaudado >= inversión Samuel)
 * - T2: Recuperar inversión vendedor + Ganancias excedentes (trigger: stock ≤10%)
 * - T3: Ganancias puras (trigger: stock ≤20%)
 * ═══════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculadorCuadreService {

    private final VentaRepository ventaRepository;
    private final CuadreRepository cuadreRepository;
    private final UsuarioRepository usuarioRepository;

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Calcula el cuadre para una tanda.
     */
    @Transactional(readOnly = true)
    public CalculoCuadreResponse calcular(Tanda tanda, TipoCuadre tipo) {
        Lote lote = tanda.getLote();
        String modelo = lote.getModelo();
        int totalTandas = lote.getNumeroTandas();

        // Recaudado de esta tanda
        BigDecimal recaudadoTanda = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());
        if (recaudadoTanda == null) recaudadoTanda = BigDecimal.ZERO;

        // Excedente del cuadre anterior (se arrastra)
        BigDecimal excedenteAnterior = cuadreRepository.obtenerUltimoExcedente(lote.getId())
                .orElse(BigDecimal.ZERO);

        BigDecimal disponibleTotal = recaudadoTanda.add(excedenteAnterior);

        CalculoCuadreResponse.CalculoCuadreResponseBuilder builder = CalculoCuadreResponse.builder()
                .tipo(tipo)
                .modelo(modelo)
                .totalRecaudado(recaudadoTanda)
                .excedenteAnterior(excedenteAnterior)
                .disponibleTotal(disponibleTotal);

        List<String> pasos = new ArrayList<>();
        pasos.add(String.format("📊 Tanda %d de %d", tanda.getNumero(), totalTandas));
        pasos.add(String.format("Total recaudado en tanda: $%s", formatMoney(recaudadoTanda)));
        
        if (excedenteAnterior.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(String.format("➕ Excedente del cuadre anterior: $%s", formatMoney(excedenteAnterior)));
            pasos.add(String.format("💰 Disponible total: $%s", formatMoney(disponibleTotal)));
        }

        // Determinar tipo de cuadre según tanda y cantidad de tandas
        if (tanda.getNumero() == 1) {
            // Tanda 1: Siempre es cuadre de inversión de Samuel
            return calcularCuadreInversionSamuel(builder, lote, disponibleTotal, pasos);
        } else if (totalTandas == 2 && tanda.getNumero() == 2) {
            // 2 tandas: T2 = inversión vendedor + ganancias
            return calcularCuadreT2_DosTandas(builder, lote, disponibleTotal, pasos);
        } else if (totalTandas == 3 && tanda.getNumero() == 2) {
            // 3 tandas: T2 = inversión vendedor + ganancias excedentes
            return calcularCuadreT2_TresTandas(builder, lote, disponibleTotal, pasos);
        } else {
            // T3 (última tanda en lotes de 3): ganancias puras
            return calcularCuadreGanancias(builder, lote, disponibleTotal, pasos);
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * TANDA 1: Cuadre de inversión de Samuel.
     * 
     * LÓGICA:
     * 1. Disponible = Recaudado en T1
     * 2. Vendedor transfiere la inversión de Samuel (50%)
     * 3. Excedente pasa ÍNTEGRO a T2 (NO se usa para inversión vendedor aquí)
     * ═══════════════════════════════════════════════════════════════════
     */
    private CalculoCuadreResponse calcularCuadreInversionSamuel(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionSamuel = lote.getInversionSamuel();
        BigDecimal inversionVendedor = lote.getInversionVendedor();
        BigDecimal inversionTotal = lote.getInversionPercibidaTotal();

        pasos.add("");
        pasos.add("═══ CUADRE TANDA 1: INVERSIÓN SAMUEL ═══");
        pasos.add(String.format("Inversión total del lote: $%s", formatMoney(inversionTotal)));
        pasos.add(String.format("• Inversión Samuel (50%%): $%s", formatMoney(inversionSamuel)));
        pasos.add(String.format("• Inversión vendedor (50%%): $%s (se recupera en T2)", formatMoney(inversionVendedor)));

        BigDecimal debeTransferir = inversionSamuel;
        BigDecimal excedente = BigDecimal.ZERO;

        if (disponible.compareTo(debeTransferir) >= 0) {
            // Hay suficiente para cubrir inversión de Samuel
            excedente = disponible.subtract(debeTransferir);
            
            if (excedente.compareTo(BigDecimal.ZERO) > 0) {
                pasos.add(String.format("✅ Recaudado suficiente para inversión Samuel"));
                pasos.add(String.format("✨ Excedente para T2: $%s", formatMoney(excedente)));
                pasos.add("📌 Este excedente pasa a T2 para recuperar inversión del vendedor");
            }
        } else {
            pasos.add(String.format("⚠️ ATENCIÓN: Disponible ($%s) < Inversión Samuel ($%s)", 
                    formatMoney(disponible), formatMoney(inversionSamuel)));
            pasos.add("El cuadre aún no puede completarse.");
        }

        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL: $%s", formatMoney(debeTransferir)));
        pasos.add(String.format("👤 VENDEDOR SE QUEDA CON: $0 (aún debe recuperar su inversión en T2)"));

        return builder
                .inversionSamuel(inversionSamuel)
                .inversionVendedor(inversionVendedor)
                .montoQueDebeTransferir(debeTransferir)
                .montoParaVendedor(BigDecimal.ZERO) // En T1 vendedor no recibe nada
                .excedenteResultante(excedente)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * TANDA 2 (en lotes de 2 tandas): Inversión vendedor + Ganancias.
     * 
     * LÓGICA:
     * 1. Disponible = Recaudado T2 + Excedente T1
     * 2. Primero: Vendedor recupera su inversión (50%)
     * 3. Restante = Ganancias → se reparten según modelo (60/40 o 50/50)
     * 4. Vendedor transfiere a Samuel su parte de ganancias
     * ═══════════════════════════════════════════════════════════════════
     */
    private CalculoCuadreResponse calcularCuadreT2_DosTandas(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionVendedor = lote.getInversionVendedor();
        int porcentajeGananciaSamuel = lote.getPorcentajeGananciaSamuel();
        int porcentajeGananciaVendedor = lote.getPorcentajeGananciaVendedor();

        pasos.add("");
        pasos.add("═══ CUADRE TANDA 2 (FINAL): INVERSIÓN + GANANCIAS ═══");
        pasos.add(String.format("Inversión vendedor pendiente: $%s", formatMoney(inversionVendedor)));
        pasos.add(String.format("Modelo ganancias: %d/%d", porcentajeGananciaVendedor, porcentajeGananciaSamuel));

        BigDecimal montoVendedor = BigDecimal.ZERO;
        BigDecimal debeTransferir = BigDecimal.ZERO;
        BigDecimal ganancias = BigDecimal.ZERO;

        if (disponible.compareTo(inversionVendedor) >= 0) {
            // Vendedor recupera toda su inversión
            montoVendedor = inversionVendedor;
            ganancias = disponible.subtract(inversionVendedor);
            
            pasos.add(String.format("✅ Vendedor recupera TODA su inversión: $%s", formatMoney(inversionVendedor)));
            
            if (ganancias.compareTo(BigDecimal.ZERO) > 0) {
                pasos.add("");
                pasos.add(String.format("💰 GANANCIAS GENERADAS: $%s", formatMoney(ganancias)));
                pasos.add(String.format("📊 Distribución %d/%d:", porcentajeGananciaVendedor, porcentajeGananciaSamuel));
                
                // Calcular distribución de ganancias según modelo
                BigDecimal gananciaSamuel = ganancias
                        .multiply(BigDecimal.valueOf(porcentajeGananciaSamuel))
                        .divide(CIEN, 0, RoundingMode.HALF_UP);
                BigDecimal gananciaVendedor = ganancias.subtract(gananciaSamuel);
                
                montoVendedor = montoVendedor.add(gananciaVendedor);
                debeTransferir = gananciaSamuel;
                
                pasos.add(String.format("  • Vendedor (%d%%): $%s", porcentajeGananciaVendedor, formatMoney(gananciaVendedor)));
                pasos.add(String.format("  • Samuel (%d%%): $%s", porcentajeGananciaSamuel, formatMoney(gananciaSamuel)));
            } else {
                pasos.add("Sin ganancias adicionales (solo recuperó inversión)");
            }
        } else {
            // No alcanza para inversión completa
            montoVendedor = disponible;
            BigDecimal faltante = inversionVendedor.subtract(disponible);
            pasos.add(String.format("⚠️ Vendedor recupera parcial: $%s", formatMoney(disponible)));
            pasos.add(String.format("📌 Falta por recuperar: $%s", formatMoney(faltante)));
            pasos.add("❌ Sin ganancias (no recuperó inversión completa)");
        }

        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL: $%s", formatMoney(debeTransferir)));
        pasos.add(String.format("👤 TOTAL VENDEDOR: $%s", formatMoney(montoVendedor)));
        pasos.add("🎉 ¡LOTE COMPLETADO!");

        return builder
                .inversionVendedor(inversionVendedor)
                .gananciasBrutas(ganancias)
                .porcentajeVendedor(BigDecimal.valueOf(porcentajeGananciaVendedor))
                .porcentajeSamuel(BigDecimal.valueOf(porcentajeGananciaSamuel))
                .montoQueDebeTransferir(debeTransferir)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * TANDA 2 (en lotes de 3 tandas): Inversión vendedor + Ganancias excedentes.
     * 
     * LÓGICA:
     * 1. Disponible = Recaudado T2 + Excedente T1
     * 2. Primero: Vendedor recupera su inversión (50%)
     * 3. Excedente sobre inversión = GANANCIAS (se reparten según modelo)
     * 4. Vendedor transfiere a Samuel su parte de ganancias
     * ═══════════════════════════════════════════════════════════════════
     */
    private CalculoCuadreResponse calcularCuadreT2_TresTandas(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionVendedor = lote.getInversionVendedor();
        int porcentajeGananciaSamuel = lote.getPorcentajeGananciaSamuel();
        int porcentajeGananciaVendedor = lote.getPorcentajeGananciaVendedor();

        pasos.add("");
        pasos.add("═══ CUADRE TANDA 2: INVERSIÓN + GANANCIAS ═══");
        pasos.add(String.format("Inversión vendedor pendiente: $%s", formatMoney(inversionVendedor)));
        pasos.add(String.format("Modelo ganancias: %d/%d", porcentajeGananciaVendedor, porcentajeGananciaSamuel));

        BigDecimal montoVendedor = BigDecimal.ZERO;
        BigDecimal debeTransferir = BigDecimal.ZERO;
        BigDecimal ganancias = BigDecimal.ZERO;

        if (disponible.compareTo(inversionVendedor) >= 0) {
            // Vendedor recupera toda su inversión
            montoVendedor = inversionVendedor;
            ganancias = disponible.subtract(inversionVendedor);
            
            pasos.add(String.format("✅ Vendedor recupera TODA su inversión: $%s", formatMoney(inversionVendedor)));
            pasos.add("🔔 ¡ALERTA! Ya recuperó su inversión. De aquí en adelante son GANANCIAS.");
            
            if (ganancias.compareTo(BigDecimal.ZERO) > 0) {
                pasos.add("");
                pasos.add(String.format("💰 GANANCIAS GENERADAS: $%s", formatMoney(ganancias)));
                pasos.add(String.format("📊 Distribución %d/%d:", porcentajeGananciaVendedor, porcentajeGananciaSamuel));
                
                // Calcular distribución de ganancias según modelo
                BigDecimal gananciaSamuel = ganancias
                        .multiply(BigDecimal.valueOf(porcentajeGananciaSamuel))
                        .divide(CIEN, 0, RoundingMode.HALF_UP);
                BigDecimal gananciaVendedor = ganancias.subtract(gananciaSamuel);
                
                montoVendedor = montoVendedor.add(gananciaVendedor);
                debeTransferir = gananciaSamuel;
                
                pasos.add(String.format("  • Vendedor (%d%%): $%s", porcentajeGananciaVendedor, formatMoney(gananciaVendedor)));
                pasos.add(String.format("  • Samuel (%d%%): $%s", porcentajeGananciaSamuel, formatMoney(gananciaSamuel)));
            } else {
                pasos.add("Sin ganancias adicionales en esta tanda");
            }
        } else {
            // No alcanza para inversión completa
            montoVendedor = disponible;
            BigDecimal faltante = inversionVendedor.subtract(disponible);
            pasos.add(String.format("⚠️ Vendedor recupera parcial: $%s", formatMoney(disponible)));
            pasos.add(String.format("📌 Falta por recuperar: $%s", formatMoney(faltante)));
            pasos.add("❌ Sin ganancias aún (no ha recuperado inversión completa)");
        }

        pasos.add("");
        if (debeTransferir.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(String.format("💵 DEBE TRANSFERIR (%d%% ganancias): $%s", 
                    porcentajeGananciaSamuel, formatMoney(debeTransferir)));
        } else {
            pasos.add("💵 NADA QUE TRANSFERIR (solo recuperación de inversión)");
        }
        pasos.add(String.format("👤 TOTAL VENDEDOR: $%s", formatMoney(montoVendedor)));
        pasos.add("✅ Con cuadre exitoso se libera Tanda 3 (ganancias puras)");

        return builder
                .inversionVendedor(inversionVendedor)
                .gananciasBrutas(ganancias)
                .porcentajeVendedor(BigDecimal.valueOf(porcentajeGananciaVendedor))
                .porcentajeSamuel(BigDecimal.valueOf(porcentajeGananciaSamuel))
                .montoQueDebeTransferir(debeTransferir)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * TANDA 3 (o última): Ganancias puras.
     * 
     * LÓGICA:
     * 1. Todo lo recaudado = GANANCIAS
     * 2. Se reparte según modelo (60/40 o 50/50)
     * 3. Vendedor transfiere la parte de Samuel
     * ═══════════════════════════════════════════════════════════════════
     */
    private CalculoCuadreResponse calcularCuadreGanancias(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        pasos.add("");
        pasos.add("═══ CUADRE GANANCIAS PURAS ═══");

        if ("MODELO_60_40".equals(lote.getModelo())) {
            return calcularGanancias60_40(builder, lote, disponible, pasos);
        } else {
            return calcularGananciasCascada(builder, lote, disponible, pasos);
        }
    }

    /**
     * Modelo 60/40: 60% vendedor, 40% Samuel.
     */
    private CalculoCuadreResponse calcularGanancias60_40(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal ganancias, List<String> pasos) {

        pasos.add("Modelo: 60/40 (N2 directo con Samuel)");
        pasos.add(String.format("💰 Ganancias totales: $%s", formatMoney(ganancias)));

        BigDecimal montoVendedor = ganancias
                .multiply(new BigDecimal("0.60"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal montoSamuel = ganancias.subtract(montoVendedor);

        pasos.add("");
        pasos.add("📊 Distribución:");
        pasos.add(String.format("  • Vendedor (60%%): $%s", formatMoney(montoVendedor)));
        pasos.add(String.format("  • Samuel (40%%): $%s", formatMoney(montoSamuel)));
        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR (40%%): $%s", formatMoney(montoSamuel)));
        pasos.add(String.format("👤 VENDEDOR SE QUEDA CON (60%%): $%s", formatMoney(montoVendedor)));
        pasos.add("🎉 ¡LOTE COMPLETADO!");

        return builder
                .gananciasBrutas(ganancias)
                .porcentajeVendedor(new BigDecimal("60"))
                .porcentajeSamuel(new BigDecimal("40"))
                .montoQueDebeTransferir(montoSamuel)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * Modelo 50/50 Cascada: 50% vendedor, 50% sube a Samuel.
     * 
     * LÓGICA:
     * 1. Vendedor se queda con 50%
     * 2. Vendedor transfiere 50% a Samuel
     * 3. Samuel después distribuye en cascada (esto es informativo)
     * ═══════════════════════════════════════════════════════════════════
     */
    private CalculoCuadreResponse calcularGananciasCascada(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal ganancias, List<String> pasos) {

        pasos.add("Modelo: 50/50 Cascada (N3+)");
        pasos.add(String.format("💰 Ganancias totales: $%s", formatMoney(ganancias)));

        Usuario vendedor = lote.getUsuario();
        List<CalculoCuadreResponse.DistribucionNivel> distribucion = new ArrayList<>();

        // 50% para el vendedor directo
        BigDecimal montoVendedor = ganancias
                .multiply(new BigDecimal("0.50"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal montoSamuel = ganancias.subtract(montoVendedor);

        distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                .nivel(vendedor.getNivel())
                .nombre(vendedor.getNombre())
                .porcentaje(new BigDecimal("50"))
                .monto(montoVendedor)
                .explicacion("50% directo (vendedor)")
                .build());

        pasos.add("");
        pasos.add("📊 Distribución:");
        pasos.add(String.format("  👤 %s (%s): $%s (50%%)", 
                vendedor.getNombre(), vendedor.getNivel(), formatMoney(montoVendedor)));
        pasos.add(String.format("  ⬆️ Samuel recibe: $%s (50%%)", formatMoney(montoSamuel)));

        // Calcular distribución cascada (informativo - Samuel la hace después)
        pasos.add("");
        pasos.add("📌 Samuel distribuirá en cascada:");

        Usuario actual = vendedor.getReclutador();
        BigDecimal subiendo = montoSamuel;

        while (actual != null && !actual.esAdmin()) {
            BigDecimal montoNivel = subiendo
                    .multiply(new BigDecimal("0.50"))
                    .setScale(0, RoundingMode.HALF_UP);
            subiendo = subiendo.subtract(montoNivel);

            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel(actual.getNivel())
                    .nombre(actual.getNombre())
                    .porcentaje(new BigDecimal("50"))
                    .monto(montoNivel)
                    .explicacion("50% de lo que sube")
                    .build());

            pasos.add(String.format("    ⬆️ %s (%s): $%s", 
                    actual.getNombre(), actual.getNivel(), formatMoney(montoNivel)));

            actual = actual.getReclutador();
        }

        // Lo que queda llega a Samuel (N1)
        Usuario samuel = usuarioRepository.findAdmin().orElse(null);
        if (samuel != null && subiendo.compareTo(BigDecimal.ZERO) > 0) {
            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel("N1")
                    .nombre(samuel.getNombre())
                    .monto(subiendo)
                    .explicacion("Resto que llega al tope")
                    .build());
            pasos.add(String.format("    ⬆️ Samuel (N1): $%s", formatMoney(subiendo)));
        }

        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL (50%%): $%s", formatMoney(montoSamuel)));
        pasos.add(String.format("👤 VENDEDOR SE QUEDA CON (50%%): $%s", formatMoney(montoVendedor)));
        pasos.add("🎉 ¡LOTE COMPLETADO!");

        return builder
                .gananciasBrutas(ganancias)
                .porcentajeVendedor(new BigDecimal("50"))
                .porcentajeSamuel(new BigDecimal("50"))
                .distribucionCascada(distribucion)
                .montoQueDebeTransferir(montoSamuel)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * Verifica si hay suficiente recaudado para cuadrar Tanda 1.
     * T1 se cuadra por MONTO, no por porcentaje de stock.
     */
    @Transactional(readOnly = true)
    public boolean puedeHacerCuadreTanda1(Tanda tanda) {
        if (tanda.getNumero() != 1) return false;
        
        BigDecimal recaudado = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());
        if (recaudado == null) recaudado = BigDecimal.ZERO;
        
        // En T1 no hay excedente anterior (es la primera tanda)
        BigDecimal inversionSamuel = tanda.getLote().getInversionSamuel();
        
        return recaudado.compareTo(inversionSamuel) >= 0;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
