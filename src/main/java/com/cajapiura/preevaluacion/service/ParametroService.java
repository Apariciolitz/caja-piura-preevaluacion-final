package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.model.ParametroSistema;
import com.cajapiura.preevaluacion.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parámetros de negocio editables por el administrador (regla de pre-evaluación y límites del simulador).
 * Si un parámetro no existe en la base, se usa su valor por defecto.
 */
@Service
public class ParametroService {

    public static final String RATIO_LIMITE = "ratio.limite";
    public static final String TASA_DEFECTO = "simulador.tasa.defecto";
    public static final String MONTO_MIN = "simulador.monto.min";
    public static final String MONTO_MAX = "simulador.monto.max";
    public static final String PLAZO_MIN = "simulador.plazo.min";
    public static final String PLAZO_MAX = "simulador.plazo.max";

    /** Clave -> {valor por defecto, descripción}. Mantiene el orden de presentación. */
    private static final Map<String, String[]> DEFINICION = new LinkedHashMap<>();
    static {
        DEFINICION.put(RATIO_LIMITE, new String[]{"4", "Ratio máximo monto/ingreso para quedar PRECALIFICADO (regla central)"});
        DEFINICION.put(TASA_DEFECTO, new String[]{"24", "Tasa anual nominal (%) que propone el simulador por defecto"});
        DEFINICION.put(MONTO_MIN, new String[]{"500", "Monto mínimo permitido en el simulador (S/)"});
        DEFINICION.put(MONTO_MAX, new String[]{"200000", "Monto máximo permitido en el simulador (S/)"});
        DEFINICION.put(PLAZO_MIN, new String[]{"3", "Plazo mínimo del simulador (meses)"});
        DEFINICION.put(PLAZO_MAX, new String[]{"60", "Plazo máximo del simulador (meses)"});
    }

    private final ParametroSistemaRepository repository;
    private final AuditoriaService auditoria;

    public ParametroService(ParametroSistemaRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    public static Map<String, String[]> definicion() {
        return DEFINICION;
    }

    /** Crea en la base los parámetros que falten (arranque). */
    public void asegurarPorDefecto() {
        DEFINICION.forEach((clave, def) -> {
            if (!repository.existsById(clave)) {
                ParametroSistema p = new ParametroSistema(clave, def[0], def[1]);
                p.setActualizadoPor("sistema");
                repository.save(p);
            }
        });
    }

    public String valor(String clave) {
        return repository.findById(clave).map(ParametroSistema::getValor).orElse(DEFINICION.get(clave)[0]);
    }

    public BigDecimal ratioLimite() { return new BigDecimal(valor(RATIO_LIMITE).trim()); }
    public BigDecimal tasaDefecto() { return new BigDecimal(valor(TASA_DEFECTO).trim()); }
    public BigDecimal montoMin() { return new BigDecimal(valor(MONTO_MIN).trim()); }
    public BigDecimal montoMax() { return new BigDecimal(valor(MONTO_MAX).trim()); }
    public int plazoMin() { return Integer.parseInt(valor(PLAZO_MIN).trim()); }
    public int plazoMax() { return Integer.parseInt(valor(PLAZO_MAX).trim()); }

    /** Valores actuales de todos los parámetros, en orden de presentación. */
    public Map<String, String> actuales() {
        Map<String, String> mapa = new LinkedHashMap<>();
        DEFINICION.keySet().forEach(k -> mapa.put(k, valor(k)));
        return mapa;
    }

    public List<ParametroSistema> listar() {
        List<ParametroSistema> lista = new ArrayList<>();
        for (String clave : DEFINICION.keySet()) {
            lista.add(repository.findById(clave).orElseGet(() ->
                    new ParametroSistema(clave, DEFINICION.get(clave)[0], DEFINICION.get(clave)[1])));
        }
        return lista;
    }

    /**
     * Valida todo el conjunto de valores propuestos. Devuelve la lista de errores (vacía si es válido).
     * Es estática y sin dependencias para poder probarla sin base de datos.
     */
    public static List<String> validar(Map<String, String> propuestos) {
        List<String> errores = new ArrayList<>();
        BigDecimal ratio = decimal(propuestos, RATIO_LIMITE, errores);
        BigDecimal tasa = decimal(propuestos, TASA_DEFECTO, errores);
        BigDecimal montoMin = decimal(propuestos, MONTO_MIN, errores);
        BigDecimal montoMax = decimal(propuestos, MONTO_MAX, errores);
        BigDecimal plazoMin = decimal(propuestos, PLAZO_MIN, errores);
        BigDecimal plazoMax = decimal(propuestos, PLAZO_MAX, errores);
        if (!errores.isEmpty()) {
            return errores;
        }
        if (ratio.compareTo(new BigDecimal("0.5")) < 0 || ratio.compareTo(new BigDecimal("20")) > 0) {
            errores.add("El ratio límite debe estar entre 0.5 y 20");
        }
        if (tasa.compareTo(BigDecimal.ZERO) < 0 || tasa.compareTo(new BigDecimal("100")) > 0) {
            errores.add("La tasa por defecto debe estar entre 0 y 100");
        }
        if (montoMin.compareTo(new BigDecimal("100")) < 0) {
            errores.add("El monto mínimo no puede ser menor a 100");
        }
        if (montoMax.compareTo(new BigDecimal("1000000")) > 0) {
            errores.add("El monto máximo no puede superar 1,000,000");
        }
        if (montoMin.compareTo(montoMax) >= 0) {
            errores.add("El monto mínimo debe ser menor al monto máximo");
        }
        if (plazoMin.compareTo(BigDecimal.ONE) < 0 || plazoMin.stripTrailingZeros().scale() > 0) {
            errores.add("El plazo mínimo debe ser un número entero de al menos 1 mes");
        }
        if (plazoMax.compareTo(new BigDecimal("120")) > 0 || plazoMax.stripTrailingZeros().scale() > 0) {
            errores.add("El plazo máximo debe ser un número entero de hasta 120 meses");
        }
        if (plazoMin.compareTo(plazoMax) >= 0) {
            errores.add("El plazo mínimo debe ser menor al plazo máximo");
        }
        return errores;
    }

    private static BigDecimal decimal(Map<String, String> valores, String clave, List<String> errores) {
        String texto = valores.get(clave);
        if (texto == null || texto.isBlank()) {
            errores.add("Falta el valor de " + clave);
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            errores.add("El valor de " + clave + " no es un número válido");
            return BigDecimal.ZERO;
        }
    }

    /**
     * Guarda los cambios (ya validados) y registra en la auditoría cada parámetro modificado.
     *
     * @return cantidad de parámetros que cambiaron
     */
    public int actualizar(Map<String, String> propuestos, String usuario) {
        List<String> errores = validar(propuestos);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
        int cambios = 0;
        for (String clave : DEFINICION.keySet()) {
            String nuevo = new BigDecimal(propuestos.get(clave).trim()).stripTrailingZeros().toPlainString();
            String anterior = valor(clave);
            if (new BigDecimal(anterior.trim()).compareTo(new BigDecimal(nuevo)) == 0) {
                continue;
            }
            ParametroSistema p = repository.findById(clave)
                    .orElseGet(() -> new ParametroSistema(clave, nuevo, DEFINICION.get(clave)[1]));
            p.setValor(nuevo);
            p.setActualizado(LocalDateTime.now());
            p.setActualizadoPor(usuario);
            repository.save(p);
            auditoria.registrar(usuario, AccionAuditoria.PARAMETROS_ACTUALIZADOS, clave, anterior + " → " + nuevo);
            cambios++;
        }
        return cambios;
    }
}
