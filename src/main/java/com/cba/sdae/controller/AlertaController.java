package com.cba.sdae.controller;

import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.service.AlertaService;
import com.cba.sdae.service.RelatorioService;
import com.cba.sdae.service.SensorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/alertas")
public class AlertaController {

    private final AlertaService    alertaService;
    private final SensorService    sensorService;
    private final RelatorioService relatorioService;

    public AlertaController(AlertaService alertaService,
                            SensorService sensorService,
                            RelatorioService relatorioService) {
        this.alertaService    = alertaService;
        this.sensorService    = sensorService;
        this.relatorioService = relatorioService;
    }

    // CORRIGIDO: injeta todos os atributos que relatorio/relatorios.html espera
    @GetMapping
    public String listar(Model model) {
        var alertas  = alertaService.listarTodos();
        var sensores = sensorService.listarTodos();

        long totalNormal  = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.NORMAL).count();
        long totalAtencao = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.ATENCAO).count();
        long totalCritico = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.CRITICO).count();

        model.addAttribute("alertas",      alertas);
        model.addAttribute("sensores",     sensores);
        model.addAttribute("relatorios",   relatorioService.listarTodos());
        model.addAttribute("totalNormal",  totalNormal);
        model.addAttribute("totalAtencao", totalAtencao);
        model.addAttribute("totalCritico", totalCritico);

        return "relatorio/relatorios";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        var alertas  = alertaService.listarTodos();
        var sensores = sensorService.listarTodos();

        long totalNormal  = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.NORMAL).count();
        long totalAtencao = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.ATENCAO).count();
        long totalCritico = alertas.stream().filter(a -> a.getNivelRisco() == NivelRisco.CRITICO).count();

        alertaService.buscarPorId(id).ifPresent(a -> model.addAttribute("alertaSelecionado", a));

        model.addAttribute("alertas",      alertas);
        model.addAttribute("sensores",     sensores);
        model.addAttribute("relatorios",   relatorioService.listarTodos());
        model.addAttribute("totalNormal",  totalNormal);
        model.addAttribute("totalAtencao", totalAtencao);
        model.addAttribute("totalCritico", totalCritico);

        return "relatorio/relatorios";
    }

    // CORRIGIDO (S5): era @GetMapping — exclusão nunca deve ser feita por GET
    // (fica vulnerável a CSRF via link/prefetch e viola a semântica HTTP).
    // O template correspondente (relatorio/relatorios.html) precisa passar a
    // enviar um <form method="post"> em vez de um <a href="...">.
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id) {
        alertaService.deletar(id);
        return "redirect:/relatorios";
    }
}