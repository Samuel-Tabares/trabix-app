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
 * Servicio para calcular cuadres según el modelo de negocio.
 * 
 * MODELO DE INVERSIÓN:
 * - N2 (MODELO_60_40): Vendedor pone 60%, Samuel 40%
 * - N3+ (MODELO_50_50): Vendedor pone 50%, Samuel 50%
 * 
 * MODELO DE GANANCIAS:
 * - MODELO_60_40: 60% vendedor, 40% Samuel
 * - MODELO_50_50 CASCADA: 50% vendedor, 50% sube en cascada
 * 
 * TANDAS:
 * - 2 tandas (< 50 TRABIX): T1 = inversión Samuel, T2 = inversión vendedor + ganancias
 * - 3 tandas (>= 50 TRABIX): T1 = inversión Samuel, T2 = inversión vendedor, T3 = ganancias
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
            // 3 tandas: T2 = inversión vendedor
            return calcularCuadreInversionVendedor(builder, lote, disponibleTotal, pasos);
        } else {
            // T3 (o última tanda): ganancias puras
            return calcularCuadreGanancias(builder, lote, disponibleTotal, pasos);
        }
    }

    /**
     * Tanda 1: Cuadre de inversión de Samuel.
     * El vendedor debe transferir la inversión de Samuel.
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
        pasos.add(String.format("• Inversión Samuel (%d%%): $%s", 
                lote.getPorcentajeInversionSamuel(), formatMoney(inversionSamuel)));
        pasos.add(String.format("• Inversión vendedor (%d%%): $%s", 
                lote.getPorcentajeInversionVendedor(), formatMoney(inversionVendedor)));

        BigDecimal debeTransferir = inversionSamuel;
        BigDecimal excedente = BigDecimal.ZERO;
        BigDecimal montoVendedor = BigDecimal.ZERO;

        if (disponible.compareTo(debeTransferir) > 0) {
            excedente = disponible.subtract(debeTransferir);
            pasos.add(String.format("✨ Excedente después de inversión Samuel: $%s", formatMoney(excedente)));

            // El excedente puede cubrir parte de la inversión del vendedor
            if (excedente.compareTo(inversionVendedor) >= 0) {
                montoVendedor = inversionVendedor;
                excedente = excedente.subtract(inversionVendedor);
                pasos.add(String.format("✅ Vendedor recupera toda su inversión: $%s", formatMoney(inversionVendedor)));
                pasos.add(String.format("✨ Excedente restante: $%s", formatMoney(excedente)));
            } else if (excedente.compareTo(BigDecimal.ZERO) > 0) {
                montoVendedor = excedente;
                pasos.add(String.format("📌 Vendedor recupera parcial: $%s de $%s", 
                        formatMoney(excedente), formatMoney(inversionVendedor)));
                excedente = BigDecimal.ZERO;
            }
        } else if (disponible.compareTo(debeTransferir) < 0) {
            pasos.add(String.format("⚠️ ATENCIÓN: Disponible ($%s) < Inversión Samuel ($%s)", 
                    formatMoney(disponible), formatMoney(inversionSamuel)));
            pasos.add("El cuadre aún no puede completarse.");
        }

        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL: $%s", formatMoney(debeTransferir)));

        return builder
                .inversionSamuel(inversionSamuel)
                .inversionVendedor(inversionVendedor)
                .montoQueDebeTransferir(debeTransferir)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(excedente)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * Tanda 2 en lotes de 2 tandas: Inversión vendedor + Ganancias.
     */
    private CalculoCuadreResponse calcularCuadreT2_DosTandas(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionVendedor = lote.getInversionVendedor();

        pasos.add("");
        pasos.add("═══ CUADRE TANDA 2 (2 TANDAS): INVERSIÓN + GANANCIAS ═══");
        pasos.add(String.format("Inversión vendedor pendiente: $%s", formatMoney(inversionVendedor)));

        BigDecimal restanteDespuesInversion;
        BigDecimal montoVendedor = BigDecimal.ZERO;
        BigDecimal debeTransferir;

        if (disponible.compareTo(inversionVendedor) >= 0) {
            // Vendedor recupera su inversión completa
            montoVendedor = inversionVendedor;
            restanteDespuesInversion = disponible.subtract(inversionVendedor);
            pasos.add(String.format("✅ Vendedor recupera inversión: $%s", formatMoney(inversionVendedor)));
            pasos.add(String.format("💰 Restante para ganancias: $%s", formatMoney(restanteDespuesInversion)));

            // Lo restante son ganancias, se reparten según modelo
            if (restanteDespuesInversion.compareTo(BigDecimal.ZERO) > 0) {
                return calcularGananciasConInversion(builder, lote, restanteDespuesInversion, 
                        montoVendedor, pasos);
            } else {
                pasos.add("Sin ganancias adicionales.");
                debeTransferir = BigDecimal.ZERO;
            }
        } else {
            // No alcanza para inversión completa
            montoVendedor = disponible;
            BigDecimal faltante = inversionVendedor.subtract(disponible);
            pasos.add(String.format("⚠️ Vendedor recupera parcial: $%s", formatMoney(disponible)));
            pasos.add(String.format("📌 Falta por recuperar: $%s", formatMoney(faltante)));
            debeTransferir = BigDecimal.ZERO;
        }

        return builder
                .inversionVendedor(inversionVendedor)
                .montoQueDebeTransferir(debeTransferir)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * Tanda 2 en lotes de 3 tandas: Inversión vendedor + Ganancias.
     * 
     * LÓGICA:
     * 1. Disponible = Excedente cuadre 1 + Recaudado tanda 2
     * 2. Primero: Vendedor recupera su inversión
     * 3. Después: Lo restante = GANANCIAS → transfiere 40% o 50% según modelo
     */
    private CalculoCuadreResponse calcularCuadreInversionVendedor(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionVendedor = lote.getInversionVendedor();
        int porcentajeGananciaSamuel = lote.getPorcentajeGananciaSamuel();
        int porcentajeGananciaVendedor = lote.getPorcentajeGananciaVendedor();

        pasos.add("");
        pasos.add("═══ CUADRE TANDA 2: INVERSIÓN + GANANCIAS ═══");
        pasos.add(String.format("Inversión vendedor pendiente: $%s", formatMoney(inversionVendedor)));

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
                pasos.add(String.format("💰 Ganancias generadas: $%s", formatMoney(ganancias)));
                
                // Calcular distribución de ganancias según modelo
                BigDecimal gananciaSamuel = ganancias
                        .multiply(BigDecimal.valueOf(porcentajeGananciaSamuel))
                        .divide(CIEN, 0, RoundingMode.HALF_UP);
                BigDecimal gananciaVendedor = ganancias.subtract(gananciaSamuel);
                
                montoVendedor = montoVendedor.add(gananciaVendedor);
                debeTransferir = gananciaSamuel;
                
                pasos.add(String.format("📊 Distribución ganancias (%d/%d):", 
                        porcentajeGananciaVendedor, porcentajeGananciaSamuel));
                pasos.add(String.format("  • Vendedor (%d%%): $%s", porcentajeGananciaVendedor, formatMoney(gananciaVendedor)));
                pasos.add(String.format("  • Samuel (%d%%): $%s", porcentajeGananciaSamuel, formatMoney(gananciaSamuel)));
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
            pasos.add("💵 NADA QUE TRANSFERIR (aún recuperando inversión)");
        }
        pasos.add(String.format("👤 TOTAL VENDEDOR: $%s", formatMoney(montoVendedor)));

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
     * Tanda 3 (o última): Ganancias puras.
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
     * Calcula ganancias con inversión ya cubierta (para T2 de 2 tandas).
     */
    private CalculoCuadreResponse calcularGananciasConInversion(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal ganancias, BigDecimal inversionRecuperada,
            List<String> pasos) {

        pasos.add("");
        pasos.add("═══ DISTRIBUCIÓN DE GANANCIAS ═══");
        pasos.add(String.format("Ganancias a repartir: $%s", formatMoney(ganancias)));

        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();
        int porcentajeSamuel = lote.getPorcentajeGananciaSamuel();

        BigDecimal gananciaVendedor = ganancias
                .multiply(BigDecimal.valueOf(porcentajeVendedor))
                .divide(CIEN, 0, RoundingMode.HALF_UP);
        BigDecimal gananciaSamuel = ganancias.subtract(gananciaVendedor);

        pasos.add(String.format("• Vendedor (%d%%): $%s", porcentajeVendedor, formatMoney(gananciaVendedor)));
        pasos.add(String.format("• Samuel (%d%%): $%s", porcentajeSamuel, formatMoney(gananciaSamuel)));

        BigDecimal totalVendedor = inversionRecuperada.add(gananciaVendedor);
        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL: $%s", formatMoney(gananciaSamuel)));
        pasos.add(String.format("👤 TOTAL VENDEDOR (inversión + ganancia): $%s", formatMoney(totalVendedor)));

        return builder
                .gananciasBrutas(ganancias)
                .porcentajeVendedor(BigDecimal.valueOf(porcentajeVendedor))
                .porcentajeSamuel(BigDecimal.valueOf(porcentajeSamuel))
                .montoQueDebeTransferir(gananciaSamuel)
                .montoParaVendedor(totalVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    /**
     * Modelo 60/40: 60% vendedor, 40% Samuel.
     */
    private CalculoCuadreResponse calcularGanancias60_40(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal ganancias, List<String> pasos) {

        pasos.add("Modelo: 60/40 (N2 directo)");

        BigDecimal montoVendedor = ganancias
                .multiply(new BigDecimal("0.60"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal montoSamuel = ganancias.subtract(montoVendedor);

        pasos.add(String.format("• Vendedor (60%%): $%s", formatMoney(montoVendedor)));
        pasos.add(String.format("• Samuel (40%%): $%s", formatMoney(montoSamuel)));
        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR (40%%): $%s", formatMoney(montoSamuel)));

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
     * Modelo 50/50 Cascada: 50% vendedor, 50% sube en cascada.
     * El vendedor transfiere el 50% a Samuel, quien distribuye en cascada.
     */
    private CalculoCuadreResponse calcularGananciasCascada(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal ganancias, List<String> pasos) {

        pasos.add("Modelo: 50/50 Cascada (N3+)");

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

        pasos.add(String.format("👤 %s (%s): $%s (50%%)", 
                vendedor.getNombre(), vendedor.getNivel(), formatMoney(montoVendedor)));

        // El 50% que sube se distribuye en cascada por Samuel
        pasos.add(String.format("⬆️ Samuel recibe para cascada: $%s (50%%)", formatMoney(montoSamuel)));
        pasos.add("");
        pasos.add("📊 Samuel distribuye en cascada:");

        // Calcular distribución cascada (informativo)
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

            pasos.add(String.format("  ⬆️ %s (%s): $%s", 
                    actual.getNombre(), actual.getNivel(), formatMoney(montoNivel)));

            actual = actual.getReclutador();
        }

        // Lo que queda llega a Samuel
        Usuario samuel = usuarioRepository.findAdmin().orElse(null);
        if (samuel != null && subiendo.compareTo(BigDecimal.ZERO) > 0) {
            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel("N1")
                    .nombre(samuel.getNombre())
                    .monto(subiendo)
                    .explicacion("Resto que llega al tope")
                    .build());
            pasos.add(String.format("  ⬆️ Samuel (N1): $%s", formatMoney(subiendo)));
        }

        pasos.add("");
        pasos.add(String.format("💵 DEBE TRANSFERIR (50%%): $%s", formatMoney(montoSamuel)));
        pasos.add(String.format("👤 VENDEDOR SE QUEDA CON (50%%): $%s", formatMoney(montoVendedor)));

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
     */
    @Transactional(readOnly = true)
    public boolean puedeHacerCuadreTanda1(Tanda tanda) {
        if (tanda.getNumero() != 1) return false;
        
        BigDecimal recaudado = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());
        if (recaudado == null) recaudado = BigDecimal.ZERO;
        
        BigDecimal excedenteAnterior = cuadreRepository.obtenerUltimoExcedente(tanda.getLote().getId())
                .orElse(BigDecimal.ZERO);
        
        BigDecimal disponible = recaudado.add(excedenteAnterior);
        BigDecimal inversionSamuel = tanda.getLote().getInversionSamuel();
        
        return disponible.compareTo(inversionSamuel) >= 0;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
