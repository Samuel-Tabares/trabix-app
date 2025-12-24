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
 * Formateados según la especificación técnica de TRABIX.
 */
@Slf4j
@Service
public class WhatsAppTextService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Genera el texto WhatsApp para un cuadre.
     */
    public String generarTexto(Tanda tanda, CalculoCuadreResponse calculo) {
        if (calculo.getTipo() == TipoCuadre.INVERSION) {
            return generarTextoInversion(tanda, calculo);
        } else {
            return generarTextoGanancias(tanda, calculo);
        }
    }

    /**
     * Genera texto para cuadre de INVERSIÓN.
     * Formato según especificación:
     * 
     * ■ CUADRE INVERSIÓN ■
     * RECOGIDOS ${monto_recaudado}
     * DEBES PASARME ${monto_inversion_samuel}
     * EXCEDENTE ${excedente}
     * ■ Con cuadre exitoso #{numero_cuadre} se libera Tanda {siguiente_tanda}
     */
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
            sb.append(String.format("📊 *Disponible total:* $%s\n", formatMoney(calculo.getDisponibleTotal())));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📤 *DEBES PASARME:* $%s\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        sb.append("(Inversión de Samuel - 50%)\n\n");
        
        if (calculo.getExcedenteResultante() != null && calculo.getExcedenteResultante().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("✨ *EXCEDENTE:* $%s\n", formatMoney(calculo.getExcedenteResultante())));
            sb.append("(Se arrastra al siguiente cuadre)\n\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("✅ Con cuadre exitoso #1 se libera *Tanda %d*\n", siguienteTanda));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Genera texto para cuadre de GANANCIAS.
     * Formato según especificación:
     * 
     * ■ CUADRE GANANCIAS ■
     * RECOGIDOS GANANCIAS ${monto_ganancia}
     * (incluye excedente anterior)
     * ME DEBES PASAR {porcentaje}% ${monto_a_transferir}
     * ■ Con cuadre exitoso #{numero_cuadre} se libera Tanda {siguiente_tanda}
     */
    private String generarTextoGanancias(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        int numeroCuadre = tanda.getNumero();
        boolean esUltimaTanda = tanda.getNumero() == 3;

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE GANANCIAS ■■\n\n");
        
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append(String.format("📊 *Tanda:* %d de 3\n", tanda.getNumero()));
        sb.append(String.format("💼 *Modelo:* %s\n\n", "MODELO_60_40".equals(lote.getModelo()) ? "60/40" : "50/50 Cascada"));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECOGIDOS GANANCIAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
        
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("(Incluye excedente anterior: $%s)\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Distribución según modelo
        if ("MODELO_60_40".equals(lote.getModelo())) {
            sb.append("📊 *DISTRIBUCIÓN 60/40:*\n");
            sb.append(String.format("  • Tu parte (60%%): $%s\n", formatMoney(calculo.getMontoParaVendedor())));
            sb.append(String.format("  • Samuel (40%%): $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
            
            sb.append(String.format("📤 *ME DEBES PASAR (40%%):* $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        } else {
            // Modelo cascada
            sb.append("📊 *DISTRIBUCIÓN CASCADA:*\n");
            if (calculo.getDistribucionCascada() != null) {
                for (CalculoCuadreResponse.DistribucionNivel nivel : calculo.getDistribucionCascada()) {
                    String icono = nivel.getNivel().equals(vendedor.getNivel()) ? "👤" : "⬆️";
                    sb.append(String.format("  %s %s (%s): $%s\n", 
                            icono, nivel.getNombre(), nivel.getNivel(), formatMoney(nivel.getMonto())));
                }
            }
            sb.append("\n");
            
            sb.append("⚠️ *REGLA CASCADA:*\n");
            sb.append("Todo el dinero va primero a @llaves\n");
            sb.append("Samuel distribuye según cascada\n\n");
            
            sb.append(String.format("📤 *DEBES TRANSFERIR TODO:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
            sb.append(String.format("📥 *Recibirás de vuelta:* $%s\n\n", formatMoney(calculo.getMontoParaVendedor())));
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        
        if (esUltimaTanda) {
            sb.append("🎉 *¡LOTE COMPLETADO!*\n");
            sb.append("Este es el último cuadre del lote.\n");
        } else {
            sb.append(String.format("✅ Con cuadre exitoso #%d se libera *Tanda %d*\n", 
                    numeroCuadre, tanda.getNumero() + 1));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Genera texto de notificación cuando el vendedor recupera su inversión.
     */
    public String generarTextoRecuperacionInversion(Usuario vendedor, BigDecimal inversionRecuperada) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 *¡FELICITACIONES!* 🎉\n\n");
        sb.append(String.format("Hola %s,\n\n", vendedor.getNombre().split(" ")[0]));
        sb.append("Has recuperado tu inversión inicial.\n");
        sb.append(String.format("💵 Inversión: $%s\n\n", formatMoney(inversionRecuperada)));
        sb.append("*A partir de ahora, todo lo que vendas es GANANCIA* 💪\n\n");
        sb.append("¡Sigue así! 🍧");
        return sb.toString();
    }

    /**
     * Genera texto de notificación cuando el stock llega al 20%.
     */
    public String generarTextoAlertaStock(Tanda tanda) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ *ALERTA DE STOCK* ⚠️\n\n");
        sb.append(String.format("Vendedor: %s\n", vendedor.getNombre()));
        sb.append(String.format("Tanda %d - Stock al %.0f%%\n\n", 
                tanda.getNumero(), tanda.getPorcentajeStockRestante()));
        sb.append(String.format("Quedan %d de %d unidades\n\n", 
                tanda.getStockActual(), tanda.getStockEntregado()));
        sb.append("📊 Se requiere cuadre próximamente.");
        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
