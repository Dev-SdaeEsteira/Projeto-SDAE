package com.cba.sdae.controller;

import com.cba.sdae.model.LeituraHistorico;
import com.cba.sdae.repository.LeituraHistoricoRepository;
import com.cba.sdae.service.EsteiraService;
import com.cba.sdae.service.SensorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller responsável pela tela de visualização e filtragem do histórico de leituras (Req. 19).
 */
@Controller
public class LeiturasController {

    private final LeituraHistoricoRepository leituraRepo;
    private final EsteiraService             esteiraService;
    private final SensorService              sensorService;

    public LeiturasController(LeituraHistoricoRepository leituraRepo,
                              EsteiraService esteiraService,
                              SensorService sensorService) {
        this.leituraRepo    = leituraRepo;
        this.esteiraService = esteiraService;
        this.sensorService  = sensorService;
    }

    @GetMapping("/leituras")
    public String paginaLeituras(@RequestParam(required = false) Long esteiraId,
                                 @RequestParam(required = false) Long sensorId,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "30") int tamanho,
                                 Model model) {

        int limit = Math.clamp(tamanho, 10, 100);
        int page = Math.max(0, pagina);

        List<LeituraHistorico> leituras = leituraRepo.filtrarLeituras(
                esteiraId, sensorId, PageRequest.of(page, limit));

        model.addAttribute("leituras",          leituras);
        model.addAttribute("esteiras",          esteiraService.listarTodas());
        model.addAttribute("sensores",          sensorService.listarTodos());
        model.addAttribute("esteiraSelecionada", esteiraId);
        model.addAttribute("sensorSelecionado",  sensorId);
        model.addAttribute("paginaAtual",        page);
        model.addAttribute("tamanho",            limit);
        model.addAttribute("totalLeituras",     leituraRepo.count());

        return "relatorio/leituras";
    }
}
