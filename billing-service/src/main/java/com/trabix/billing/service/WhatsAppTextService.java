package com.trabix.billing.service;

import com.trabix.billing.dto.CalculoCuadreResponse;
import com.trabix.billing.entity.Lote;
import com.trabix.billing.entity.Tanda;
import com.trabix.billing.entity.Usuario;
import com.trabix.common.enums.TipoCuadre;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.*;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar textos listos para enviar por WhatsApp.
 */
@Slf4j
@Service
public class WhatsAppTextService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String generarTexto(Tanda tanda, CalculoCuadreResponse calculo) {
        int totalTandas = tanda.getTotalTandas();
        int numeroTanda = tanda.getNumero();

        if (numeroTanda == 1) {
            return generarTextoInversionSamuel(tanda, calculo, totalTandas);
        } else if (totalTandas == 2 && numeroTanda == 2) {
            return generarTextoT2_DosTandas(tanda, calculo);
        } else if (totalTandas == 3 && numeroTanda == 2) {
            return generarTextoInversionVendedor(tanda, calculo);
        } else {
            return generarTextoGanancias(tanda, calculo);
        }
    }

    /**
     * Tanda 1: Cuadre de inversión de Samuel.
     */
    private String generarTextoInversionSamuel(Tanda tanda, CalculoCuadreResponse calculo, int totalTandas) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 1: INVERSIÓN SAMUEL ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d (%d unidades)\n", lote.getId(), lote.getCantidadTotal()));
        sb.append(String.format("📊 *Tanda:* 1 de %d\n", totalTandas));
        sb.append(String.format("💼 *Modelo inversión:* %d/%d\n\n", 
                lote.getPorcentajeInversionVendedor(), lote.getPorcentajeInversionSamuel()));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECAUDADO:* $%s\n", formatMoney(calculo.getTotalRecaudado())));
        
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("➕ *Excedente anterior:* $%s\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📤 *DEBES PASARME:* $%s\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        sb.append(String.format("(Inversión de Samuel - %d%%)\n\n", lote.getPorcentajeInversionSamuel()));
        
        if (calculo.getMontoParaVendedor() != null && calculo.getMontoParaVendedor().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("👤 *Tu parte (excedente):* $%s\n\n", formatMoney(calculo.getMontoParaVendedor())));
        }
        
        if (calculo.getExcedenteResultante() != null && calculo.getExcedenteResultante().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("✨ *Excedente para siguiente:* $%s\n\n", formatMoney(calculo.getExcedenteResultante())));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("✅ Con cuadre exitoso se libera *Tanda 2*\n"));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Tanda 2 en lotes de 2 tandas: Inversión vendedor + Ganancias.
     */
    private String generarTextoT2_DosTandas(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 2 (FINAL): INVERSIÓN + GANANCIAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append(String.format("📊 *Tanda:* 2 de 2 (FINAL)\n"));
        sb.append(String.format("💼 *Modelo ganancias:* %s\n\n", 
                lote.esModelo60_40() ? "60/40" : "50/50 Cascada"));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECAUDADO:* $%s\n", formatMoney(calculo.getTotalRecaudado())));
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("➕ *Excedente anterior:* $%s\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append(String.format("👤 *TU PARTE TOTAL:* $%s\n", formatMoney(calculo.getMontoParaVendedor())));
        sb.append("(Inversión recuperada + ganancia)\n\n");

        if (calculo.getMontoQueDebeTransferir().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("📤 *DEBES PASARME:* $%s\n", formatMoney(calculo.getMontoQueDebeTransferir())));
            sb.append(String.format("(%d%% de ganancias)\n\n", lote.getPorcentajeGananciaSamuel()));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🎉 *¡LOTE COMPLETADO!*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Tanda 2 en lotes de 3 tandas: Inversión vendedor + Ganancias.
     */
    private String generarTextoInversionVendedor(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        int porcentajeSamuel = lote.getPorcentajeGananciaSamuel();
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 2: INVERSIÓN + GANANCIAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append(String.format("📊 *Tanda:* 2 de 3\n"));
        sb.append(String.format("💼 *Modelo ganancias:* %d/%d\n\n", porcentajeVendedor, porcentajeSamuel));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *DISPONIBLE:* $%s\n", formatMoney(calculo.getDisponibleTotal())));
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (Incluye excedente cuadre 1: $%s)\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Mostrar inversión recuperada
        if (calculo.getInversionVendedor() != null) {
            sb.append(String.format("✅ *INVERSIÓN RECUPERADA:* $%s\n", formatMoney(calculo.getInversionVendedor())));
            sb.append("🔔 ¡Ya recuperaste tu inversión!\n\n");
        }

        // Mostrar ganancias si hay
        if (calculo.getGananciasBrutas() != null && calculo.getGananciasBrutas().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("💰 *GANANCIAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
            sb.append(String.format("📊 Distribución %d/%d:\n", porcentajeVendedor, porcentajeSamuel));
            
            BigDecimal tuParte = calculo.getGananciasBrutas()
                    .multiply(BigDecimal.valueOf(porcentajeVendedor))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            sb.append(String.format("  • Tu parte (%d%%): $%s\n", porcentajeVendedor, formatMoney(tuParte)));
            sb.append(String.format("  • Samuel (%d%%): $%s\n\n", porcentajeSamuel, formatMoney(calculo.getMontoQueDebeTransferir())));
        }

        sb.append(String.format("👤 *TOTAL PARA TI:* $%s\n", formatMoney(calculo.getMontoParaVendedor())));
        sb.append("(Inversión + tu parte de ganancias)\n\n");

        if (calculo.getMontoQueDebeTransferir() != null && calculo.getMontoQueDebeTransferir().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("📤 *DEBES PASARME (%d%% ganancias):* $%s\n\n", 
                    porcentajeSamuel, formatMoney(calculo.getMontoQueDebeTransferir())));
        } else {
            sb.append("📤 *NADA QUE TRANSFERIR*\n");
            sb.append("(Aún recuperando inversión)\n\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ Con cuadre exitoso se libera *Tanda 3* (ganancias puras)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Tanda 3 (o última): Ganancias puras.
     */
    private String generarTextoGanancias(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        boolean esCascada = lote.esModelo50_50();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 3: GANANCIAS PURAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append(String.format("📊 *Tanda:* 3 de 3 (FINAL)\n"));
        sb.append(String.format("💼 *Modelo:* %s\n\n", esCascada ? "50/50 Cascada" : "60/40"));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *GANANCIAS BRUTAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (esCascada) {
            sb.append("📊 *DISTRIBUCIÓN CASCADA:*\n");
            if (calculo.getDistribucionCascada() != null) {
                for (CalculoCuadreResponse.DistribucionNivel nivel : calculo.getDistribucionCascada()) {
                    String icono = nivel.getNivel().equals(vendedor.getNivel()) ? "👤" : "⬆️";
                    sb.append(String.format("  %s %s (%s): $%s\n", 
                            icono, nivel.getNombre(), nivel.getNivel(), formatMoney(nivel.getMonto())));
                }
            }
            sb.append("\n⚠️ *REGLA CASCADA:* Todo va a Samuel\n");
            sb.append(String.format("📤 *TRANSFERIR TODO:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
            sb.append(String.format("📥 *Recibirás de Samuel:* $%s\n\n", formatMoney(calculo.getMontoParaVendedor())));
        } else {
            sb.append("📊 *DISTRIBUCIÓN 60/40:*\n");
            sb.append(String.format("  👤 Tu parte (60%%): $%s\n", formatMoney(calculo.getMontoParaVendedor())));
            sb.append(String.format("  ⬆️ Samuel (40%%): $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
            sb.append(String.format("📤 *ME DEBES PASAR (40%%):* $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🎉 *¡LOTE COMPLETADO!*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Genera texto de alerta de stock bajo en Tanda 1.
     */
    public String generarTextoAlertaStock(Tanda tanda) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        BigDecimal inversionSamuel = lote.getInversionSamuel();

        return String.format("""
            ⚠️ *ALERTA DE STOCK - TANDA 1* ⚠️
            
            Vendedor: %s (%s)
            Lote: #%d
            Tanda 1 de %d - Stock al %.0f%%
            
            Quedan %d de %d unidades
            
            📊 Inversión Samuel a recuperar: $%s
            
            ⚠️ El cuadre se genera cuando el recaudado
            sea >= a la inversión de Samuel.
            
            📅 %s
            🍧 TRABIX Granizados""",
            vendedor.getNombre(), vendedor.getNivel(),
            lote.getId(), lote.getNumeroTandas(),
            tanda.getPorcentajeStockRestante(),
            tanda.getStockActual(), tanda.getStockEntregado(),
            formatMoney(inversionSamuel),
            LocalDateTime.now().format(FORMATO_FECHA));
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
