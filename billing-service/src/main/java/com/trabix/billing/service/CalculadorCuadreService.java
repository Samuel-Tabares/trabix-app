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
 * MODELO 60/40 (N2): 60% vendedor, 40% Samuel
 * MODELO 50/50 CASCADA (N3+): Toddo va a Samuel, él distribuye
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculadorCuadreService {

    private final VentaRepository ventaRepository;
    private final CuadreRepository cuadreRepository;
    private final UsuarioRepository usuarioRepository;

    private static final BigDecimal PORCENTAJE_VENDEDOR_60_40 = new BigDecimal("0.60");
    private static final BigDecimal PORCENTAJE_CASCADA = new BigDecimal("0.50");

    @Transactional(readOnly = true)
    public CalculoCuadreResponse calcular(Tanda tanda, TipoCuadre tipo) {
        Lote lote = tanda.getLote();
        String modelo = lote.getModelo();

        BigDecimal recaudadoTanda = ventaRepository.sumarRecaudadoPorTanda(tanda.getId());
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

    private CalculoCuadreResponse calcularCuadreInversion(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        BigDecimal inversionSamuel = lote.getInversionSamuel();
        BigDecimal inversionVendedor = lote.getInversionVendedor();

        pasos.add("--- CUADRE DE INVERSIÓN ---");
        pasos.add(String.format("Inversión total del lote: $%s", formatMoney(lote.getInversionPercibidaTotal())));
        pasos.add(String.format("Inversión de Samuel (50%%): $%s", formatMoney(inversionSamuel)));
        pasos.add(String.format("Inversión del vendedor (50%%): $%s", formatMoney(inversionVendedor)));

        BigDecimal debeTransferir = inversionSamuel;
        BigDecimal excedente = BigDecimal.ZERO;
        BigDecimal montoVendedor = BigDecimal.ZERO;

        if (disponible.compareTo(debeTransferir) > 0) {
            excedente = disponible.subtract(debeTransferir);
            pasos.add(String.format("Excedente: $%s", formatMoney(excedente)));

            if (excedente.compareTo(inversionVendedor) >= 0) {
                montoVendedor = inversionVendedor;
                excedente = excedente.subtract(inversionVendedor);
                pasos.add(String.format("✅ Vendedor recupera inversión: $%s", formatMoney(inversionVendedor)));
            } else {
                montoVendedor = excedente;
                excedente = BigDecimal.ZERO;
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

    private CalculoCuadreResponse calcularCuadreGanancias60_40(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        pasos.add("--- CUADRE DE GANANCIAS (60/40) ---");

        BigDecimal ganancias = disponible;
        BigDecimal montoVendedor = ganancias.multiply(PORCENTAJE_VENDEDOR_60_40)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal montoSamuel = ganancias.subtract(montoVendedor);

        pasos.add(String.format("60%% para vendedor: $%s", formatMoney(montoVendedor)));
        pasos.add(String.format("40%% para Samuel: $%s", formatMoney(montoSamuel)));
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

    private CalculoCuadreResponse calcularCuadreGananciasCascada(
            CalculoCuadreResponse.CalculoCuadreResponseBuilder builder,
            Lote lote, BigDecimal disponible, List<String> pasos) {

        pasos.add("--- CUADRE DE GANANCIAS (50/50 CASCADA) ---");

        BigDecimal ganancias = disponible;
        Usuario vendedor = lote.getUsuario();
        List<CalculoCuadreResponse.DistribucionNivel> distribucion = new ArrayList<>();

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

            pasos.add(String.format("  → %s (%s): $%s", actual.getNombre(), actual.getNivel(), formatMoney(montoNivel)));
            actual = actual.getReclutador();
        }

        Usuario samuel = usuarioRepository.findAdmin().orElse(null);
        if (samuel != null) {
            distribucion.add(CalculoCuadreResponse.DistribucionNivel.builder()
                    .nivel("N1")
                    .nombre(samuel.getNombre())
                    .monto(subiendo)
                    .explicacion("Resto que llega al tope")
                    .build());
            pasos.add(String.format("  → Samuel (N1): $%s", formatMoney(subiendo)));
        }

        pasos.add(String.format("💵 TRANSFERIR TODO: $%s", formatMoney(ganancias)));

        return builder
                .gananciasBrutas(ganancias)
                .distribucionCascada(distribucion)
                .montoQueDebeTransferir(ganancias)
                .montoParaVendedor(montoVendedor)
                .excedenteResultante(BigDecimal.ZERO)
                .pasosCalculo(pasos)
                .build();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
