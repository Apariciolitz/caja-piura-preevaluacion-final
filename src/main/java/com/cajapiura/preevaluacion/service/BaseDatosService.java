package com.cajapiura.preevaluacion.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Explorador de base de datos de SOLO LECTURA para el administrador. Nunca ejecuta SQL escrito por el
 * usuario: el nombre de tabla se valida contra el catálogo real de la base y los valores se leen con
 * una conexión marcada como solo lectura. Las columnas de contraseñas se enmascaran.
 */
@Service
public class BaseDatosService {

    public static final int TAMANO_PAGINA = 25;
    public static final int MAX_EXPORTACION = 10_000;
    private static final int MAX_CELDA = 200;
    private static final String OCULTO = "•••• (oculto)";

    public static class TablaInfo {
        private final String nombre;
        private final long filas;

        public TablaInfo(String nombre, long filas) {
            this.nombre = nombre;
            this.filas = filas;
        }

        public String getNombre() { return nombre; }
        public long getFilas() { return filas; }
    }

    public static class ColumnaInfo {
        private final String nombre;
        private final String tipo;
        private final boolean nulable;
        private final boolean clavePrimaria;

        public ColumnaInfo(String nombre, String tipo, boolean nulable, boolean clavePrimaria) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.nulable = nulable;
            this.clavePrimaria = clavePrimaria;
        }

        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
        public boolean isNulable() { return nulable; }
        public boolean isClavePrimaria() { return clavePrimaria; }
    }

    public static class Pagina {
        private final String tabla;
        private final List<ColumnaInfo> columnas;
        private final List<String> encabezados;
        private final List<List<String>> filas;
        private final int pagina;
        private final long total;

        public Pagina(String tabla, List<ColumnaInfo> columnas, List<String> encabezados,
                      List<List<String>> filas, int pagina, long total) {
            this.tabla = tabla;
            this.columnas = columnas;
            this.encabezados = encabezados;
            this.filas = filas;
            this.pagina = pagina;
            this.total = total;
        }

        public String getTabla() { return tabla; }
        public List<ColumnaInfo> getColumnas() { return columnas; }
        public List<String> getEncabezados() { return encabezados; }
        public List<List<String>> getFilas() { return filas; }
        public int getPagina() { return pagina; }
        public long getTotal() { return total; }
        public int getTotalPaginas() { return (int) Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA); }
        public boolean isHayAnterior() { return pagina > 1; }
        public boolean isHaySiguiente() { return pagina < getTotalPaginas(); }
    }

    private final DataSource dataSource;

    public BaseDatosService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Una columna es sensible si su nombre sugiere contraseña o hash. */
    public static boolean esSensible(String columna) {
        String c = columna == null ? "" : columna.toLowerCase(Locale.ROOT);
        return c.contains("password") || c.contains("hash") || c.contains("secret") || c.contains("token");
    }

    private Connection conectar() throws SQLException {
        Connection c = dataSource.getConnection();
        c.setReadOnly(true);
        return c;
    }

    public String motor() {
        try (Connection c = conectar()) {
            DatabaseMetaData m = c.getMetaData();
            return m.getDatabaseProductName() + " " + m.getDatabaseProductVersion();
        } catch (SQLException e) {
            return "No disponible";
        }
    }

    public List<TablaInfo> tablas() {
        List<TablaInfo> lista = new ArrayList<>();
        try (Connection c = conectar()) {
            String esquema = c.getSchema();
            DatabaseMetaData meta = c.getMetaData();
            String q = meta.getIdentifierQuoteString();
            try (ResultSet rs = meta.getTables(c.getCatalog(), esquema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String nombre = rs.getString("TABLE_NAME");
                    lista.add(new TablaInfo(nombre, contar(c, q, nombre)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer el catálogo de la base de datos", e);
        }
        lista.sort(java.util.Comparator.comparing(TablaInfo::getNombre));
        return lista;
    }

    private long contar(Connection c, String q, String tabla) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + q + tabla + q)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Devuelve el nombre exacto de la tabla si existe en el catálogo; si no, lanza excepción. */
    private String validarTabla(Connection c, String pedida) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getTables(c.getCatalog(), c.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String nombre = rs.getString("TABLE_NAME");
                if (nombre.equalsIgnoreCase(pedida)) {
                    return nombre;
                }
            }
        }
        throw new IllegalArgumentException("La tabla no existe");
    }

    public Pagina pagina(String tablaPedida, int pagina) {
        int p = Math.max(1, pagina);
        try (Connection c = conectar()) {
            String tabla = validarTabla(c, tablaPedida);
            String q = c.getMetaData().getIdentifierQuoteString();
            long total = contar(c, q, tabla);
            List<ColumnaInfo> columnas = columnas(c, tabla);
            String sql = "SELECT * FROM " + q + tabla + q + " ORDER BY 1 LIMIT ? OFFSET ?";
            List<String> encabezados = new ArrayList<>();
            List<List<String>> filas = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, TAMANO_PAGINA);
                ps.setLong(2, (long) (p - 1) * TAMANO_PAGINA);
                try (ResultSet rs = ps.executeQuery()) {
                    leer(rs, encabezados, filas, true);
                }
            }
            return new Pagina(tabla, columnas, encabezados, filas, p, total);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la tabla", e);
        }
    }

    /** CSV de la tabla (hasta MAX_EXPORTACION filas), con columnas sensibles enmascaradas. */
    public String exportarCsv(String tablaPedida) {
        try (Connection c = conectar()) {
            String tabla = validarTabla(c, tablaPedida);
            String q = c.getMetaData().getIdentifierQuoteString();
            List<String> encabezados = new ArrayList<>();
            List<List<String>> filas = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + q + tabla + q + " ORDER BY 1 LIMIT ?")) {
                ps.setInt(1, MAX_EXPORTACION);
                try (ResultSet rs = ps.executeQuery()) {
                    leer(rs, encabezados, filas, false);
                }
            }
            StringBuilder sb = new StringBuilder(CsvUtil.fila(encabezados));
            for (List<String> fila : filas) {
                sb.append(CsvUtil.fila(fila));
            }
            return sb.toString();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo exportar la tabla", e);
        }
    }

    private List<ColumnaInfo> columnas(Connection c, String tabla) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        List<String> pk = new ArrayList<>();
        try (ResultSet rs = meta.getPrimaryKeys(c.getCatalog(), c.getSchema(), tabla)) {
            while (rs.next()) {
                pk.add(rs.getString("COLUMN_NAME"));
            }
        }
        List<ColumnaInfo> lista = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), c.getSchema(), tabla, "%")) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                lista.add(new ColumnaInfo(nombre, rs.getString("TYPE_NAME"),
                        rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls, pk.contains(nombre)));
            }
        }
        return lista;
    }

    private void leer(ResultSet rs, List<String> encabezados, List<List<String>> filas, boolean recortar) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        boolean[] sensible = new boolean[n];
        for (int i = 0; i < n; i++) {
            String nombre = md.getColumnLabel(i + 1);
            encabezados.add(nombre);
            sensible[i] = esSensible(nombre);
        }
        while (rs.next()) {
            List<String> fila = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                if (sensible[i]) {
                    fila.add(OCULTO);
                    continue;
                }
                Object v = rs.getObject(i + 1);
                String texto = v == null ? "" : String.valueOf(v);
                fila.add(recortar && texto.length() > MAX_CELDA ? texto.substring(0, MAX_CELDA) + "…" : texto);
            }
            filas.add(fila);
        }
    }
}
