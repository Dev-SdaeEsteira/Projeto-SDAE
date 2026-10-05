package com.cba.sdae.controller;

import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.service.EsteiraService;
import com.cba.sdae.service.FuncionarioService;
import com.cba.sdae.service.SensorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sensores")
public class SensorController {

    private final SensorService      sensorService;
    private final EsteiraService     esteiraService;
    private final FuncionarioService funcionarioService;

    public SensorController(SensorService sensorService,
                            EsteiraService esteiraService,
                            FuncionarioService funcionarioService) {
        this.sensorService      = sensorService;
        this.esteiraService     = esteiraService;
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var sensores = sensorService.listarTodos();

        model.addAttribute("sensores",      sensores);
        model.addAttribute("totalSensores", sensores.size());
        model.addAttribute("totalNormal",   sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.NORMAL).count());
        model.addAttribute("totalAtencao",  sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.ATENCAO).count());
        model.addAttribute("totalCritico",  sensores.stream().filter(s -> s.getNivelRisco() == NivelRisco.CRITICO).count());
        model.addAttribute("totalConectados", sensores.stream().filter(Sensor::isConectado).count());

        if (userDetails != null) {
            funcionarioService.buscarPorLogin(userDetails.getUsername())
                    .ifPresentOrElse(
                            f -> model.addAttribute("nomeUsuario", f.getNome()),
                            () -> model.addAttribute("nomeUsuario", userDetails.getUsername())
                    );
        }
        return "sensor/sensores";
    }

    @GetMapping("/novo")
    public String formularioNovo(Model model) {
        model.addAttribute("sensor",   new Sensor());
        model.addAttribute("esteiras", esteiraService.listarTodas());
        return "cadastro/cadastrosensor";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        var sensor = sensorService.buscarPorId(id);
        if (sensor.isEmpty()) return "redirect:/sensores";
        model.addAttribute("sensor",   sensor.get());
        model.addAttribute("esteiras", esteiraService.listarTodas());
        return "cadastro/editarsensor";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Sensor sensor,
                         @RequestParam(required = false) Long esteiraId) {
        boolean eraNovo = sensor.getId() == null;
        String erroRedirect = eraNovo
                ? "redirect:/sensores/novo?erro=true"
                : "redirect:/sensores/editar/" + sensor.getId() + "?erro=true";
        try {
            if (esteiraId != null) {
                var esteira = esteiraService.buscarPorId(esteiraId);
                if (esteira.isEmpty()) return erroRedirect;
                sensor.setEsteira(esteira.get());
            }
            if (sensor.getEsteira() == null) return erroRedirect;

            Sensor salvo = sensorService.salvar(sensor);

            return eraNovo
                    ? "redirect:/sensores/editar/" + salvo.getId() + "?criado=true"
                    : "redirect:/sensores";

        } catch (Exception e) {
            return erroRedirect;
        }
    }

    // CORRIGIDO (S5): era @GetMapping — ver mesma observação em AlertaController.
    // O template sensor/sensores.html precisa passar a usar um
    // <form method="post"> em vez de <a href="...">.
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id) {
        sensorService.deletar(id);
        return "redirect:/sensores";
    }

    /**
     * Exibe o detalhe de um sensor com status de conexão e histórico de leituras
     */
    @GetMapping("/detalhe/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        var sensor = sensorService.buscarPorId(id);
        if (sensor.isEmpty()) return "redirect:/sensores";
        
        Sensor s = sensor.get();
        model.addAttribute("sensor", s);
        var historico = sensorService.listarHistorico(id, 30);
        java.util.Collections.reverse(historico);
        model.addAttribute("historico", historico);
        return "sensor/detalhe-sensor";
    }
}