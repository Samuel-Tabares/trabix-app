package com.trabix.billing.service;

import com.trabix.billing.dto.CalculoCuadreResponse;
import com.trabix.billing.entity.Lote;
import com.trabix.billing.entity.Tanda;
import com.trabix.billing.entity.Usuario;
import com.trabix.common.enums.TipoCuadre;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        if (calculo.getTipo() == TipoCuadre.INVERSION) {
            return generarTextoInversion(tanda, calculo);
        } else {
            return generarTextoGanancias(tanda, calculo);
        }
    }

    private String generarTextoInversion(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        int siguienteTanda = tanda.getNumero() + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE INVERSIÓN ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d (%d unidades)\n", lote.getId(), lote.getCantidadTotal()));
        sb.append(String.format("📊 *Tanda:* %d de 3\n\n", tanda.getNumero()));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECOGIDOS:* $%s\n", formatMoney(calculo.getTotalRecaudado())));
        
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("➕ *Excedente anterior:* $%s\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📤 *DEBES PASARME:* $%s\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        sb.append("(Inversión de Samuel - 50%)\n\n");
        
        if (calculo.getExcedenteResultante() != null && calculo.getExcedenteResultante().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("✨ *EXCEDENTE:* $%s\n\n", formatMoney(calculo.getExcedenteResultante())));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("✅ Con cuadre exitoso #1 se libera *Tanda %d*\n", siguienteTanda));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    private String generarTextoGanancias(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        boolean esUltimaTanda = tanda.getNumero() == 3;

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE GANANCIAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append(String.format("📊 *Tanda:* %d de 3\n", tanda.getNumero()));
        sb.append(String.format("💼 *Modelo:* %s\n\n", "MODELO_60_40".equals(lote.getModelo()) ? "60/40" : "50/50 Cascada"));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECOGIDOS GANANCIAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if ("MODELO_60_40".equals(lote.getModelo())) {
            sb.append("📊 *DISTRIBUCIÓN 60/40:*\n");
            sb.append(String.format("  • Tu parte (60%%): $%s\n", formatMoney(calculo.getMontoParaVendedor())));
            sb.append(String.format("  • Samuel (40%%): $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
            sb.append(String.format("📤 *ME DEBES PASAR (40%%):* $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        } else {
            sb.append("📊 *DISTRIBUCIÓN CASCADA:*\n");
            if (calculo.getDistribucionCascada() != null) {
                for (CalculoCuadreResponse.DistribucionNivel nivel : calculo.getDistribucionCascada()) {
                    String icono = nivel.getNivel().equals(vendedor.getNivel()) ? "👤" : "⬆️";
                    sb.append(String.format("  %s %s (%s): $%s\n", icono, nivel.getNombre(), nivel.getNivel(), formatMoney(nivel.getMonto())));
                }
            }
            sb.append("\n⚠️ *REGLA CASCADA:* Todo va a @llaves\n");
            sb.append(String.format("📤 *TRANSFERIR TODO:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
            sb.append(String.format("📥 *Recibirás:* $%s\n\n", formatMoney(calculo.getMontoParaVendedor())));
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        if (esUltimaTanda) {
            sb.append("🎉 *¡LOTE COMPLETADO!*\n");
        } else {
            sb.append(String.format("✅ Con cuadre exitoso #%d se libera *Tanda %d*\n", tanda.getNumero(), tanda.getNumero() + 1));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    public String generarTextoAlertaStock(Tanda tanda) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();

        return String.format("""
            ⚠️ *ALERTA DE STOCK* ⚠️
            
            Vendedor: %s
            Tanda %d - Stock al %.0f%%
            
            Quedan %d de %d unidades
            
            📊 Se requiere cuadre próximamente.""",
            vendedor.getNombre(), tanda.getNumero(), tanda.getPorcentajeStockRestante(),
            tanda.getStockActual(), tanda.getStockEntregado());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
