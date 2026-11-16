package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Genera el comprobante PDF descargable con el resultado de la
 * pre-evaluación, para que el cliente pueda guardarlo o imprimirlo.
 */
@Service
public class PdfService {

    private static final Color COLOR_MARCA = new Color(182, 103, 44); // terracota de la identidad visual
    private static final Color COLOR_OSCURO = new Color(36, 28, 21);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarComprobante(SolicitudCredito s) {
        Document documento = new Document(PageSize.A4, 56, 56, 64, 56);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD, COLOR_OSCURO);
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.GRAY);
            Font fuenteSeccion = FontFactory.getFont(FontFactory.HELVETICA, 13, Font.BOLD, COLOR_MARCA);
            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COLOR_OSCURO);
            Font fuenteResultado = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD,
                    s.getResultado() == ResultadoEvaluacion.PRECALIFICADO ? new Color(31, 78, 74) : new Color(162, 74, 59));

            Paragraph titulo = new Paragraph("Caja Piura — Comprobante de Pre-evaluación", fuenteTitulo);
            titulo.setSpacingAfter(4);
            documento.add(titulo);

            Paragraph subtitulo = new Paragraph("Código de solicitud: #CP-" + String.format("%05d", s.getId()), fuenteSubtitulo);
            subtitulo.setSpacingAfter(18);
            documento.add(subtitulo);

            Paragraph resultado = new Paragraph(s.getResultado().getEtiqueta(), fuenteResultado);
            resultado.setSpacingAfter(2);
            documento.add(resultado);

            Paragraph subResultado = new Paragraph(s.getResultado().getSubtitulo(), fuenteEtiqueta);
            subResultado.setSpacingAfter(20);
            documento.add(subResultado);

            documento.add(new Paragraph("Datos del solicitante", fuenteSeccion));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setSpacingAfter(20);
            tabla.setWidths(new float[]{1.2f, 2f});

            agregarFila(tabla, "Nombre completo", s.getNombreCompleto(), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "DNI", s.getDni(), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "Destino del crédito", s.getDestinoCredito(), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "Ingreso mensual", "S/ " + s.getIngresoMensual(), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "Monto solicitado", "S/ " + s.getMontoSolicitado(), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "Ratio monto / ingreso", String.valueOf(s.getRatio()), fuenteEtiqueta, fuenteValor);
            agregarFila(tabla, "Fecha de evaluación",
                    s.getFechaCreacion() != null ? s.getFechaCreacion().format(FORMATO_FECHA) : "-",
                    fuenteEtiqueta, fuenteValor);

            documento.add(tabla);

            if (s.getCitaFecha() != null) {
                documento.add(new Paragraph("Cita agendada", fuenteSeccion));
                documento.add(new Paragraph(" "));
                PdfPTable tablaCita = new PdfPTable(2);
                tablaCita.setWidthPercentage(100);
                tablaCita.setWidths(new float[]{1.2f, 2f});
                agregarFila(tablaCita, "Fecha", s.getCitaFecha().toString(), fuenteEtiqueta, fuenteValor);
                agregarFila(tablaCita, "Hora", s.getCitaHora(), fuenteEtiqueta, fuenteValor);
                agregarFila(tablaCita, "Agencia", s.getCitaAgencia(), fuenteEtiqueta, fuenteValor);
                documento.add(tablaCita);
            }

            Paragraph aviso = new Paragraph(
                    "Este documento es un resultado preliminar generado automáticamente y no constituye "
                    + "una aprobación crediticia formal. La evaluación final está a cargo de un asesor de "
                    + "negocios de Caja Piura.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.GRAY));
            aviso.setSpacingBefore(24);
            documento.add(aviso);

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", e);
        }

        return salida.toByteArray();
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor, Font fuenteEtiqueta, Font fuenteValor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuenteEtiqueta));
        celdaEtiqueta.setBorderColor(new Color(231, 220, 200));
        celdaEtiqueta.setPadding(8);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor == null ? "-" : valor, fuenteValor));
        celdaValor.setBorderColor(new Color(231, 220, 200));
        celdaValor.setPadding(8);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }
}
