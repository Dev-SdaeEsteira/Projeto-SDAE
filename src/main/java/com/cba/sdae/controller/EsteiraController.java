package com.cba.sdae.controller;

import com.cba.sdae.model.Esteira;
import com.cba.sdae.repository.LeituraHistoricoRepository;
import com.cba.sdae.service.AlertaService;
import com.cba.sdae.service.EsteiraService;
import com.cba.sdae.service.EventoEsteiraService;
import com.cba.sdae.service.SensorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/esteiras")
public class EsteiraController {

    private final EsteiraService             esteiraService;
    private final SensorService              sensorService;
    private final AlertaService              alertaService;
    private final EventoEsteiraService       eventoService;
    private final LeituraHistoricoRepository leituraRepo;

    public EsteiraController(EsteiraService esteiraService,
                             SensorService sensorService,
                             AlertaService alertaService,
                             EventoEsteiraService eventoService,
                             LeituraHistoricoRepository leituraRepo) {
        this.esteiraService = esteiraService;
        this.sensorService  = sensorService;
        this.alertaService  = alertaService;
        this.eventoService  = eventoService;
        this.leituraRepo    = leituraRepo;
    }

    @GetMapping
    public String listar(Model model) {
        var esteiras = esteiraService.listarTodas();
        model.addAttribute("esteiras",          esteiras);
        model.addAttribute("totalAtivas",       esteiras.stream().filter(e -> Boolean.TRUE.equals(e.getAtiva())).count());
        model.addAttribute("totalInativas",     esteiras.stream().filter(e -> !Boolean.TRUE.equals(e.getAtiva())).count());
        model.addAttribute("totalFuncionando",  esteiraService.contarFuncionando());
        model.addAttribute("totalParadas",      esteiraService.contarParadas());
        model.addAttribute("totalOffline",      esteiraService.contarOffline());
        model.addAttribute("totalAlertas",      alertaService.contarTotal());
        model.addAttribute("totalSensores",     sensorService.contarTotal());
        return "esteiras/esteiras";
    }

    @GetMapping("/nova")
    public String formularioNova(Model model) {
        model.addAttribute("esteira", new Esteira());
        return "cadastro/cadastroesteira";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        var esteira = esteiraService.buscarPorId(id);
        if (esteira.isEmpty()) return "redirect:/esteiras";
        model.addAttribute("esteira", esteira.get());
        return "cadastro/editaresteira";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Esteira esteira) {
        Long idExistente = esteira.getId();
        try {
            if (esteira.getAtiva() == null) esteira.setAtiva(true);
            esteiraService.salvar(esteira);
            return "redirect:/esteiras";
        } catch (Exception e) {
            return idExistente != null
                    ? "redirect:/esteiras/editar/" + idExistente + "?erro=true"
                    : "redirect:/esteiras/nova?erro=true";
        }
    }

    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id) {
        esteiraService.deletar(id);
        return "redirect:/esteiras";
    }

    /**
     * Exibe o detalhe completo de uma esteira (Req. 23)
     */
    @GetMapping("/detalhe/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        var opt = esteiraService.buscarPorId(id);
        if (opt.isEmpty()) return "redirect:/esteiras";
        
        Esteira e = opt.get();
        model.addAttribute("esteira", e);
        model.addAttribute("sensoresVinculados", sensorService.listarPorEsteira(e.getId()));
        model.addAttribute("eventos", eventoService.listarPorEsteira(e.getId(), 20));
        model.addAttribute("leituras", leituraRepo.findByEsteira_IdOrderByDataHoraDesc(e.getId(), PageRequest.of(0, 30)));
        return "esteiras/detalhe-esteira";
    }

    @PostMapping("/{id}/manutencao")
    public String alternarManutencao(@PathVariable Long id,
                                     @RequestParam(defaultValue = "true") boolean entrar,
                                     @RequestParam(required = false) String motivo) {
        try {
            esteiraService.alternarManutencao(id, entrar, motivo);
        } catch (Exception ignored) {}
        return "redirect:/esteiras/detalhe/" + id;
    }
}