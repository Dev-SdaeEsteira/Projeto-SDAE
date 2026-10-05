package com.cba.sdae.controller;

import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.service.AlertaService;
import com.cba.sdae.service.EsteiraService;
import com.cba.sdae.service.EventoEsteiraService;
import com.cba.sdae.service.FuncionarioService;
import com.cba.sdae.service.SensorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final EsteiraService       esteiraService;
    private final SensorService        sensorService;
    private final AlertaService        alertaService;
    private final FuncionarioService   funcionarioService;
    private final EventoEsteiraService eventoService;

    public DashboardController(EsteiraService esteiraService,
                               SensorService sensorService,
                               AlertaService alertaService,
                               FuncionarioService funcionarioService,
                               EventoEsteiraService eventoService) {
        this.esteiraService     = esteiraService;
        this.sensorService      = sensorService;
        this.alertaService      = alertaService;
        this.funcionarioService = funcionarioService;
        this.eventoService      = eventoService;
    }

    @GetMapping({"/dashboard", "/index"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        var esteiras = esteiraService.listarTodas();
        var sensores = sensorService.listarTodos();
        var alertas  = alertaService.listarMaisRecentes(15);
        var eventos  = eventoService.listarMaisRecentes(15);

        // ── Contadores de esteiras reais (Req. 16) ──────────────────
        long totalFuncionando = esteiraService.contarFuncionando();
        long totalParadas     = esteiraService.contarParadas();
        long totalOffline     = esteiraService.contarOffline();
        long totalAtivas      = esteiraService.contarAtivas();
        long totalInativas    = esteiraService.contarInativas();

        // ── Contadores de sensores por nível de risco ─────────────
        long totalNormal  = sensorService.contarPorNivelRisco(NivelRisco.NORMAL);
        long totalAtencao = sensorService.contarPorNivelRisco(NivelRisco.ATENCAO);
        long totalCritico = sensorService.contarPorNivelRisco(NivelRisco.CRITICO);

        // ── Maior leitura atual (risco máximo) ────────────────────
        double maxRisco = sensorService.buscarMaiorLeitura();

        // ── Nome do usuário logado ────────────────────────────────
        if (userDetails != null) {
            funcionarioService.buscarPorLogin(userDetails.getUsername())
                    .ifPresentOrElse(
                            f -> model.addAttribute("nomeUsuario", f.getNome()),
                            () -> model.addAttribute("nomeUsuario", userDetails.getUsername())
                    );
        } else {
            model.addAttribute("nomeUsuario", "Usuário");
        }

        // ── Envia tudo para o HTML ────────────────────────────────
        model.addAttribute("esteiras",              esteiras);
        model.addAttribute("sensores",              sensores);
        model.addAttribute("alertas",               alertas);
        model.addAttribute("eventos",               eventos);
        model.addAttribute("totalFuncionando",       totalFuncionando);
        model.addAttribute("totalParadas",           totalParadas);
        model.addAttribute("totalOffline",           totalOffline);
        model.addAttribute("totalEsteirasAtivas",   totalAtivas);
        model.addAttribute("totalEsteirasInativas", totalInativas);
        model.addAttribute("totalAlertasHoje",      alertaService.contarTotal());
        model.addAttribute("totalSensores",         sensorService.contarTotal());
        model.addAttribute("nivelRiscoAtual",       (int) maxRisco);
        model.addAttribute("totalNormal",           totalNormal);
        model.addAttribute("totalAtencao",          totalAtencao);
        model.addAttribute("totalCritico",          totalCritico);

        return "dashboard/index";
    }
}