package com.trabix.billing.service;

import com.trabix.billing.dto.CalculoCuadreResponse;
import com.trabix.billing.entity.Lote;
import com.trabix.billing.entity.Tanda;
import com.trabix.billing.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar textos listos para enviar por WhatsApp.
 * 
 * Genera mensajes claros y bien formateados para cada tipo de cuadre.
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
            return generarTextoT2_TresTandas(tanda, calculo);
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
        sb.append("💼 *Modelo inversión:* 50/50 (siempre)\n\n");
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *RECAUDADO:* $%s\n", formatMoney(calculo.getTotalRecaudado())));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📤 *DEBES PASARME:* $%s\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        sb.append("(Inversión de Samuel - 50%)\n\n");
        
        if (calculo.getExcedenteResultante() != null && calculo.getExcedenteResultante().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("✨ *Excedente para T2:* $%s\n", formatMoney(calculo.getExcedenteResultante())));
            sb.append("(Se usará para recuperar tu inversión)\n\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("✅ Con cuadre exitoso se libera *Tanda 2*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Tanda 2 en lotes de 2 tandas: Inversión vendedor + Ganancias (FINAL).
     */
    private String generarTextoT2_DosTandas(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();
        int porcentajeSamuel = lote.getPorcentajeGananciaSamuel();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 2 (FINAL): INVERSIÓN + GANANCIAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append("📊 *Tanda:* 2 de 2 (FINAL)\n");
        sb.append(String.format("💼 *Modelo ganancias:* %d/%d\n\n", porcentajeVendedor, porcentajeSamuel));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *DISPONIBLE:* $%s\n", formatMoney(calculo.getDisponibleTotal())));
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (Incluye excedente T1: $%s)\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Inversión recuperada
        if (calculo.getInversionVendedor() != null) {
            sb.append(String.format("✅ *INVERSIÓN RECUPERADA:* $%s\n\n", formatMoney(calculo.getInversionVendedor())));
        }

        // Ganancias
        if (calculo.getGananciasBrutas() != null && calculo.getGananciasBrutas().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("💰 *GANANCIAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
            sb.append(String.format("📊 Distribución %d/%d:\n", porcentajeVendedor, porcentajeSamuel));
            
            BigDecimal tuParte = calculo.getGananciasBrutas()
                    .multiply(BigDecimal.valueOf(porcentajeVendedor))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            BigDecimal parteSamuel = calculo.getGananciasBrutas().subtract(tuParte);
            
            sb.append(String.format("  • Tu parte (%d%%): $%s\n", porcentajeVendedor, formatMoney(tuParte)));
            sb.append(String.format("  • Samuel (%d%%): $%s\n\n", porcentajeSamuel, formatMoney(parteSamuel)));
        }

        sb.append(String.format("👤 *TOTAL PARA TI:* $%s\n", formatMoney(calculo.getMontoParaVendedor())));
        sb.append("(Inversión + tu parte de ganancias)\n\n");

        if (calculo.getMontoQueDebeTransferir() != null && calculo.getMontoQueDebeTransferir().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("📤 *DEBES PASARME (%d%% ganancias):* $%s\n\n", 
                    porcentajeSamuel, formatMoney(calculo.getMontoQueDebeTransferir())));
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🎉 *¡LOTE COMPLETADO!*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append(String.format("📅 %s\n", LocalDateTime.now().format(FORMATO_FECHA)));
        sb.append("🍧 TRABIX Granizados");

        return sb.toString();
    }

    /**
     * Tanda 2 en lotes de 3 tandas: Inversión vendedor + Ganancias excedentes.
     */
    private String generarTextoT2_TresTandas(Tanda tanda, CalculoCuadreResponse calculo) {
        Lote lote = tanda.getLote();
        Usuario vendedor = lote.getUsuario();
        int porcentajeSamuel = lote.getPorcentajeGananciaSamuel();
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 2: INVERSIÓN + GANANCIAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append("📊 *Tanda:* 2 de 3\n");
        sb.append(String.format("💼 *Modelo ganancias:* %d/%d\n\n", porcentajeVendedor, porcentajeSamuel));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *DISPONIBLE:* $%s\n", formatMoney(calculo.getDisponibleTotal())));
        if (calculo.getExcedenteAnterior() != null && calculo.getExcedenteAnterior().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (Incluye excedente T1: $%s)\n", formatMoney(calculo.getExcedenteAnterior())));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Inversión recuperada
        if (calculo.getInversionVendedor() != null) {
            sb.append(String.format("✅ *INVERSIÓN RECUPERADA:* $%s\n", formatMoney(calculo.getInversionVendedor())));
            sb.append("🔔 ¡Ya recuperaste tu inversión!\n\n");
        }

        // Ganancias si hay
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
            sb.append("(Solo recuperaste tu inversión)\n\n");
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
        int porcentajeVendedor = lote.getPorcentajeGananciaVendedor();
        int porcentajeSamuel = lote.getPorcentajeGananciaSamuel();

        StringBuilder sb = new StringBuilder();
        sb.append("■■ CUADRE TANDA 3: GANANCIAS PURAS ■■\n\n");
        sb.append(String.format("🧑 *Vendedor:* %s (%s)\n", vendedor.getNombre(), vendedor.getNivel()));
        sb.append(String.format("📦 *Lote:* #%d\n", lote.getId()));
        sb.append("📊 *Tanda:* 3 de 3 (FINAL)\n");
        sb.append(String.format("💼 *Modelo:* %s\n\n", esCascada ? "50/50 Cascada" : "60/40"));
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("💰 *GANANCIAS:* $%s\n", formatMoney(calculo.getGananciasBrutas())));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (esCascada) {
            sb.append(String.format("📊 *DISTRIBUCIÓN 50/50:*\n"));
            sb.append(String.format("  👤 Tu parte (50%%): $%s\n", formatMoney(calculo.getMontoParaVendedor())));
            sb.append(String.format("  ⬆️ Samuel (50%%): $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
            
            // Mostrar distribución cascada informativa
            if (calculo.getDistribucionCascada() != null && calculo.getDistribucionCascada().size() > 1) {
                sb.append("📌 *Samuel distribuirá en cascada:*\n");
                for (int i = 1; i < calculo.getDistribucionCascada().size(); i++) {
                    CalculoCuadreResponse.DistribucionNivel nivel = calculo.getDistribucionCascada().get(i);
                    sb.append(String.format("    ⬆️ %s (%s): $%s\n", 
                            nivel.getNombre(), nivel.getNivel(), formatMoney(nivel.getMonto())));
                }
                sb.append("\n");
            }
            
            sb.append(String.format("📤 *DEBES PASARME (50%%):* $%s\n\n", formatMoney(calculo.getMontoQueDebeTransferir())));
        } else {
            sb.append(String.format("📊 *DISTRIBUCIÓN %d/%d:*\n", porcentajeVendedor, porcentajeSamuel));
            sb.append(String.format("  👤 Tu parte (%d%%): $%s\n", porcentajeVendedor, formatMoney(calculo.getMontoParaVendedor())));
            sb.append(String.format("  ⬆️ Samuel (%d%%): $%s\n\n", porcentajeSamuel, formatMoney(calculo.getMontoQueDebeTransferir())));
            sb.append(String.format("📤 *DEBES PASARME (%d%%):* $%s\n\n", porcentajeSamuel, formatMoney(calculo.getMontoQueDebeTransferir())));
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
     * (Solo informativo - T1 se cuadra por monto, no por porcentaje)
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
            
            📌 Recuerda: T1 no se cuadra por porcentaje
            de stock, sino por monto recaudado.
            
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
