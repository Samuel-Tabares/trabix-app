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
 * MODELO 60/40 (Nivel N2 - Directo con Samuel):
 * - Inversión: 50% vendedor, 50% Samuel
 * - Ganancias: 60% vendedor, 40% Samuel
 * 
 * MODELO 50/50 CASCADA (Nivel N3+):
 * - Todo el dinero va a @llaves (Samuel)
 * - 50% para el vendedor (nodo hoja)
 * - 50% sube en cascada hacia arriba
 * - Cada nivel intermedio recibe 50% de lo que sube
 * - Samuel recibe el último 50%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculadorCuadreService {

    private final VentaRepository ventaRepository;
    private final CuadreRepository cuadreRepository;
    private final UsuarioRepository usuarioRepository;

    // Porcentajes modelo 60/40
    private static final BigDecimal PORCENTAJE_VENDEDOR_60_40 = new BigDecimal("0.60");
    private static final BigDecimal PORCENTAJE_SAMUEL_60_40 = new BigDecimal("0.40");

    // Porcentaje modelo 50/50 cascada
    private static final BigDecimal PORCENTAJE_CASCADA = new BigDecimal("0.50");

    /**
     * Calcula los montos de un cuadre.
     */
    @Transactional(readOnly = true)
    public CalculoCuadreResponse calcular(Tanda tanda, TipoCuadre tipo) {
        Lote lote = tanda.getLote();
        String modelo = lote.getModelo();

        // Obtener recaudado de la tanda
        BigDecimal recaudadoTanda = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());

        // Obtener excedente del cuadre anterior (si existe)
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
        pasos.add(String.format("Total recaudado en tanda: $%s", formatMoney(recaudadoTanda)));
        
        if (excedenteAnterior.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(String.format("Excedente del cuadre anterior: $%s", formatMoney(excedenteAnterior)));
            pasos.add(String.format("Disponible total: $%s", formatMoney(disponibleTotal)));
        }

        if (tipo == TipoCuadre.INVERSION) {
            return calcularCuadreInversion(builder, lote, disponibleTotal, pasos);
        } else {
            if ("MODELO_60_40".equals(modelo)) {
                return calcularCuadreGanancias60_40(builder, lote, disponibleTotal, pasos);
            } else {
                return calcularCuadreGananciasCascada(builder, lote, disponibleTotal, pasos);
            }
        }
    }

    /**
     * Calcula cuadre de INVERSIÓN (Tanda 1).
     * El vendedor debe pasar a Samuel su parte de la inversión.
     */
    private CalculoCuadreResponse calcularCuadreInversion(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote,
            BigDecimal disponible,
            List<String> pasos) {

        BigDecimal inversionSamuel = lote.getInversionSamuel();
        BigDecimal inversionVendedor = lote.getInversionVendedor();

        pasos.add("--- CUADRE DE INVERSIÓN ---");
        pasos.add(String.format("Inversión total del lote: $%s", formatMoney(lote.getInversionPercibidaTotal())));
        pasos.add(String.format("Inversión de Samuel (50%%): $%s", formatMoney(inversionSamuel)));
        pasos.add(String.format("Inversión del vendedor (50%%): $%s", formatMoney(inversionVendedor)));

        // El vendedor debe transferir la inversión de Samuel
        BigDecimal debeTransferir = inversionSamuel;

        // Calcular excedente (lo que sobra después de pagar inversión de Samuel)
        // Este excedente se usa para que el vendedor recupere su inversión
        BigDecimal excedente = BigDecimal.ZERO;
        BigDecimal montoVendedor = BigDecimal.ZERO;

        if (disponible.compareTo(debeTransferir) > 0) {
            excedente = disponible.subtract(debeTransferir);
            pasos.add(String.format("Excedente (para recuperar inversión vendedor): $%s", formatMoney(excedente)));

            // Si el excedente cubre la inversión del vendedor, ya la recuperó
            if (excedente.compareTo(inversionVendedor) >= 0) {
                montoVendedor = inversionVendedor;
                excedente = excedente.subtract(inversionVendedor);
                pasos.add(String.format("✅ Vendedor recupera su inversión: $%s", formatMoney(inversionVendedor)));
                pasos.add(String.format("Excedente restante (ganancia): $%s", formatMoney(excedente)));
            } else {
                montoVendedor = excedente;
                excedente = BigDecimal.ZERO;
                pasos.add(String.format("Vendedor recupera parcialmente: $%s (le faltan $%s)",
                        formatMoney(montoVendedor), formatMoney(inversionVendedor.subtract(montoVendedor))));
            }
        }

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
     * Calcula cuadre de GANANCIAS modelo 60/40 (N2).
     */
    private CalculoCuadreResponse calcularCuadreGanancias60_40(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote,
            BigDecimal disponible,
            List<String> pasos) {

        pasos.add("--- CUADRE DE GANANCIAS (60/40) ---");
        pasos.add("Modelo: N2 directo con Samuel");

        // Las ganancias son todo el disponible (ya se cubrió inversión en tanda 1)
        BigDecimal ganancias = disponible;
        pasos.add(String.format("Ganancias a distribuir: $%s", formatMoney(ganancias)));

        // 60% para vendedor, 40% para Samuel
        BigDecimal montoVendedor = ganancias.multiply(PORCENTAJE_VENDEDOR_60_40)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal montoSamuel = ganancias.subtract(montoVendedor);

        pasos.add(String.format("60%% para vendedor: $%s", formatMoney(montoVendedor)));
        pasos.add(String.format("40%% para Samuel: $%s", formatMoney(montoSamuel)));

        // El vendedor ya tiene su 60%, debe transferir el 40% de Samuel
        pasos.add(String.format("💵 DEBE TRANSFERIR A SAMUEL (40%%): $%s", formatMoney(montoSamuel)));

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
     * Calcula cuadre de GANANCIAS modelo 50/50 CASCADA (N3+).
     * 
     * Ejemplo: S1 (N4) genera $100,000
     * - S1 (nodo hoja): 50% = $50,000
     * - R1 (N3, reclutador de S1): 50% de $50,000 = $25,000
     * - V1 (N2, reclutador de R1): 50% de $25,000 = $12,500
     * - Samuel (N1): Los $12,500 restantes
     */
    private CalculoCuadreResponse calcularCuadreGananciasCascada(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote,
            BigDecimal disponible,
            List<String> pasos) {

        pasos.add("--- CUADRE DE GANANCIAS (50/50 CASCADA) ---");
        pasos.add("Modelo: N3+ con distribución en cascada");

        BigDecimal ganancias = disponible;
        pasos.add(String.format("Ganancias a distribuir: $%s", formatMoney(ganancias)));

        Usuario vendedor = lote.getUsuario();
        List<CalculoCuadreResponse.DistribucionNivel> distribucion = new ArrayList<>();

        // 50% para el vendedor (nodo hoja)
        BigDecimal montoVendedor = ganancias.multiply(PORCENTAJE_CASCADA)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal restante = ganancias.subtract(montoVendedor);

        distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                .nivel(vendedor.getNivel())
                .nombre(vendedor.getNombre())
                .porcentaje(new BigDecimal("50"))
                .monto(montoVendedor)
                .explicacion("50% directo (nodo hoja)")
                .build());

        pasos.add(String.format("50%% para %s (%s): $%s",
                vendedor.getNombre(), vendedor.getNivel(), formatMoney(montoVendedor)));

        // Subir por la cascada
        Usuario actual = vendedor.getReclutador();
        BigDecimal subiendo = restante;

        while (actual != null && !actual.esAdmin()) {
            BigDecimal montoNivel = subiendo.multiply(PORCENTAJE_CASCADA)
                    .setScale(0, RoundingMode.HALF_UP);
            subiendo = subiendo.subtract(montoNivel);

            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel(actual.getNivel())
                    .nombre(actual.getNombre())
                    .porcentaje(new BigDecimal("50"))
                    .monto(montoNivel)
                    .explicacion("50% de lo que sube")
                    .build());

            pasos.add(String.format("  → %s (%s) recibe 50%% de $%s = $%s",
                    actual.getNombre(), actual.getNivel(),
                    formatMoney(montoNivel.add(subiendo)), formatMoney(montoNivel)));

            actual = actual.getReclutador();
        }

        // Lo que queda es para Samuel
        Usuario samuel = usuarioRepository.findAdmin().orElse(null);
        if (samuel != null) {
            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel("N1")
                    .nombre(samuel.getNombre())
                    .porcentaje(null)
                    .monto(subiendo)
                    .explicacion("Resto que llega al tope")
                    .build());

            pasos.add(String.format("  → Samuel (N1) recibe el resto: $%s", formatMoney(subiendo)));
        }

        // Todo el dinero va primero a @llaves (Samuel)
        // El vendedor transfiere todo, Samuel distribuye
        pasos.add("");
        pasos.add("⚠️ REGLA: Todo el dinero va primero a @llaves (Samuel)");
        pasos.add(String.format("💵 DEBE TRANSFERIR TODO: $%s", formatMoney(ganancias)));
        pasos.add(String.format("Samuel distribuirá $%s al vendedor", formatMoney(montoVendedor)));

        return builder
                .gananciasBrutas(ganancias)
                .distribucionCascada(distribucion)
                .montoQueDebeTransferir(ganancias) // Todo va a Samuel
                .montoParaVendedor(montoVendedor)  // Lo que Samuel le devuelve
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
