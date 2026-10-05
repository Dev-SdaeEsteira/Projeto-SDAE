package com.cba.sdae.service;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.model.Sensor;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    // ── Cores do sistema SDAE ─────────────────────────────────────
    private static final Color AZUL_CBA    = new Color(30, 78, 159);
    private static final Color CINZA_ESCURO = new Color(30, 41, 59);
    private static final Color CINZA_CLARO  = new Color(241, 245, 249);
    private static final Color VERDE        = new Color(16, 185, 129);
    private static final Color AMARELO      = new Color(245, 158, 11);
    private static final Color VERMELHO     = new Color(239, 68, 68);
    private static final Color BRANCO       = Color.WHITE;

    // ── Fontes ────────────────────────────────────────────────────
    private static final Font FONT_TITULO    = new Font(Font.HELVETICA, 20, Font.BOLD,   BRANCO);
    private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(148, 163, 184));
    private static final Font FONT_SECAO     = new Font(Font.HELVETICA, 12, Font.BOLD,   AZUL_CBA);
    private static final Font FONT_HEADER    = new Font(Font.HELVETICA, 9,  Font.BOLD,   BRANCO);
    private static final Font FONT_CELULA    = new Font(Font.HELVETICA, 9,  Font.NORMAL, CINZA_ESCURO);
    private static final Font FONT_BADGE     = new Font(Font.HELVETICA, 8,  Font.BOLD,   BRANCO);
    private static final Font FONT_KPI_NUM   = new Font(Font.HELVETICA, 22, Font.BOLD,   AZUL_CBA);
    private static final Font FONT_KPI_LBL   = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(100, 116, 139));

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gera o relatório completo em bytes — pronto para download.
     */
    public byte[] gerarRelatorio(List<Sensor> sensores, List<Alerta> alertas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            // Rodapé em todas as páginas
            writer.setPageEvent(new RodapePdf());

            doc.open();

            // ── Cabeçalho ─────────────────────────────────────────────
            adicionarCabecalho(doc);

            // ── KPIs resumo ───────────────────────────────────────────
            adicionarKpis(doc, sensores, alertas);

            // ── Tabela de Sensores ────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            adicionarTituloSecao(doc, "Sensores Monitorados");
            adicionarTabelaSensores(doc, sensores);

            // ── Tabela de Alertas ─────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            adicionarTituloSecao(doc, "Histórico de Alertas");
            adicionarTabelaAlertas(doc, alertas);

        } catch (DocumentException e) {
            // CORRIGIDO: DocumentException é uma exceção checked lançada por
            // PdfWriter.getInstance(...) e pelos métodos adicionarXxx(...),
            // mas antes não era capturada nem declarada no "throws" do
            // método — isso não compilava. Agora é convertida numa
            // RuntimeException para não obrigar quem chama gerarRelatorio()
            // (RelatoriosController) a tratar uma exceção checked.
            throw new RuntimeException("Erro ao gerar o PDF do relatório: " + e.getMessage(), e);
        } finally {
            // CORRIGIDO: garante que o Document/PdfWriter sejam fechados
            // mesmo se algo falhar no meio da geração, evitando vazamento
            // de recursos internos do OpenPDF.
            if (doc.isOpen()) {
                doc.close();
            }
        }

        return out.toByteArray();
    }

    // ── Cabeçalho azul com título ─────────────────────────────────
    private void adicionarCabecalho(Document doc) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(AZUL_CBA);
        cell.setPadding(20);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph titulo = new Paragraph("SDAE — Sistema de Automação de Esteiras", FONT_TITULO);
        titulo.setAlignment(Element.ALIGN_LEFT);
        cell.addElement(titulo);

        String dataAtual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        Paragraph sub = new Paragraph("Relatório Operacional · CBA — Companhia Brasileira de Alumínio · Gerado em " + dataAtual, FONT_SUBTITULO);
        sub.setSpacingBefore(4);
        cell.addElement(sub);

        header.addCell(cell);
        doc.add(header);
        doc.add(Chunk.NEWLINE);
    }

    // ── Linha de KPIs ─────────────────────────────────────────────
    private void adicionarKpis(Document doc, List<Sensor> sensores, List<Alerta> alertas) throws DocumentException {
        long normal  = sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.NORMAL).count();
        long atencao = sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.ATENCAO).count();
        long critico = sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.CRITICO).count();

        PdfPTable kpis = new PdfPTable(4);
        kpis.setWidthPercentage(100);
        kpis.setSpacingAfter(8);

        kpiCard(kpis, String.valueOf(sensores.size()), "Total de Sensores", AZUL_CBA);
        kpiCard(kpis, String.valueOf(normal),          "Normal",            VERDE);
        kpiCard(kpis, String.valueOf(atencao),         "Atenção",           AMARELO);
        kpiCard(kpis, String.valueOf(critico),         "Crítico",           VERMELHO);

        doc.add(kpis);
    }

    private void kpiCard(PdfPTable table, String numero, String label, Color cor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(14);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(CINZA_CLARO);

        Font numFont = new Font(Font.HELVETICA, 22, Font.BOLD, cor);
        Paragraph num = new Paragraph(numero, numFont);
        num.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(num);

        Paragraph lbl = new Paragraph(label, FONT_KPI_LBL);
        lbl.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(lbl);

        table.addCell(cell);
    }

    // ── Título de seção ───────────────────────────────────────────
    private void adicionarTituloSecao(Document doc, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, FONT_SECAO);
        p.setSpacingAfter(6);
        doc.add(p);

        // linha divisória
        LineSeparator linha = new LineSeparator(1f, 100f, new Color(203, 213, 225), Element.ALIGN_LEFT, -2);
        doc.add(new Chunk(linha));
        doc.add(Chunk.NEWLINE);
    }

    // ── Tabela de Sensores ────────────────────────────────────────
    private void adicionarTabelaSensores(Document doc, List<Sensor> sensores) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1f, 3f, 2f, 2f});
        table.setWidthPercentage(100);

        // Headers
        headerCell(table, "ID");
        headerCell(table, "Modelo");
        headerCell(table, "Leitura Atual");
        headerCell(table, "Nível de Risco");

        if (sensores.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum sensor cadastrado.", FONT_CELULA));
            vazio.setColspan(4);
            vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
            vazio.setPadding(10);
            table.addCell(vazio);
        } else {
            boolean linha = false;
            for (Sensor s : sensores) {
                Color bg = linha ? CINZA_CLARO : BRANCO;
                dataCell(table, String.valueOf(s.getId()), bg);
                dataCell(table, s.getModelo() != null ? s.getModelo() : "—", bg);
                dataCell(table, String.format("%.1f%%", s.getLeituraAtual() != null ? s.getLeituraAtual() : 0), bg);
                badgeCell(table, s.getNivelRisco(), bg);
                linha = !linha;
            }
        }

        doc.add(table);
    }

    // ── Tabela de Alertas ─────────────────────────────────────────
    private void adicionarTabelaAlertas(Document doc, List<Alerta> alertas) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1f, 2f, 4f, 2f});
        table.setWidthPercentage(100);

        headerCell(table, "ID");
        headerCell(table, "Data / Hora");
        headerCell(table, "Mensagem");
        headerCell(table, "Nível");

        if (alertas.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum alerta registrado.", FONT_CELULA));
            vazio.setColspan(4);
            vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
            vazio.setPadding(10);
            table.addCell(vazio);
        } else {
            boolean linha = false;
            for (Alerta a : alertas) {
                Color bg = linha ? CINZA_CLARO : BRANCO;
                dataCell(table, String.valueOf(a.getId()), bg);
                dataCell(table, a.getDataHora() != null ? a.getDataHora().format(FMT) : "—", bg);
                dataCell(table, a.getMensagem() != null ? a.getMensagem() : "—", bg);
                badgeCell(table, a.getNivelRisco(), bg);
                linha = !linha;
            }
        }

        doc.add(table);
    }

    // ── Células auxiliares ────────────────────────────────────────
    private void headerCell(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_HEADER));
        cell.setBackgroundColor(CINZA_ESCURO);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void dataCell(PdfPTable table, String texto, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CELULA));
        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void badgeCell(PdfPTable table, NivelRisco nivel, Color bg) {
        Color cor = switch (nivel != null ? nivel : NivelRisco.NORMAL) {
            case NORMAL  -> VERDE;
            case ATENCAO -> AMARELO;
            case CRITICO -> VERMELHO;
        };

        PdfPCell cell = new PdfPCell(new Phrase((nivel != null ? nivel.name() : "—"), FONT_BADGE));
        cell.setBackgroundColor(cor);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    // ── Rodapé em todas as páginas ────────────────────────────────
    static class RodapePdf extends PdfPageEventHelper {
        private static final Font FONT_RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(148, 163, 184));

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(0.5f);
            cb.setColorStroke(new Color(203, 213, 225));
            cb.moveTo(document.left(), document.bottom() - 10);
            cb.lineTo(document.right(), document.bottom() - 10);
            cb.stroke();

            Phrase rodape = new Phrase(
                    "SDAE - Sistema de Automação de Esteira Página " + writer.getPageNumber(),
                    FONT_RODAPE
            );
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, rodape,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 20, 0);
        }
    }
}