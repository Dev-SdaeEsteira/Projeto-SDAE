package com.cba.sdae.controller;

import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.service.AlertaService;
import com.cba.sdae.service.PdfService;
import com.cba.sdae.service.RelatorioService;
import com.cba.sdae.service.SensorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/relatorios")
public class RelatoriosController {

    private final RelatorioService relatorioService;
    private final AlertaService    alertaService;
    private final SensorService    sensorService;
    private final PdfService       pdfService;

    public RelatoriosController(RelatorioService relatorioService,
                                AlertaService alertaService,
                                SensorService sensorService,
                                PdfService pdfService) {
        this.relatorioService = relatorioService;
        this.alertaService    = alertaService;
        this.sensorService    = sensorService;
        this.pdfService       = pdfService;
    }

    // ── Página de relatórios ──────────────────────────────────────
    @GetMapping
    public String relatorios(Model model) {
        var alertas  = alertaService.listarTodos();
        var sensores = sensorService.listarTodos();

        long totalNormal  = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.NORMAL).count();
        long totalAtencao = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.ATENCAO).count();
        long totalCritico = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.CRITICO).count();

        model.addAttribute("relatorios",   relatorioService.listarTodos());
        model.addAttribute("alertas",      alertas);
        model.addAttribute("sensores",     sensores);
        model.addAttribute("totalNormal",  totalNormal);
        model.addAttribute("totalAtencao", totalAtencao);
        model.addAttribute("totalCritico", totalCritico);

        return "relatorio/relatorios";
    }

    // ── Download do PDF ───────────────────────────────────────────
    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        var sensores = sensorService.listarTodos();
        var alertas  = alertaService.listarTodos();

        byte[] pdf = pdfService.gerarRelatorio(sensores, alertas);

        String nomeArquivo = "SDAE_Relatorio_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}