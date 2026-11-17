package com.cajapiura.preevaluacion.service;

import java.util.List;

/** Utilidad para exportar CSV de forma segura (comillas, saltos de línea e inyección de fórmulas en Excel). */
public final class CsvUtil {

    private CsvUtil() {
    }

    public static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        // Un texto que empieza con = + - @ se interpretaría como fórmula al abrirlo en Excel.
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    public static String fila(List<?> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(celda(valores.get(i)));
        }
        return sb.append("\r\n").toString();
    }
}
