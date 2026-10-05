package com.cba.sdae.controller.rest;

import com.cba.sdae.model.Esteira;
import com.cba.sdae.service.EsteiraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/esteiras")
public class EsteiraRestController {

    private final EsteiraService esteiraService;

    public EsteiraRestController(EsteiraService esteiraService) {
        this.esteiraService = esteiraService;
    }

    @GetMapping
    public List<Esteira> listar() { return esteiraService.listarTodas(); }

    @GetMapping("/{id}")
    public ResponseEntity<Esteira> buscarPorId(@PathVariable Long id) {
        return esteiraService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Esteira> criar(@Valid @RequestBody Esteira esteira) {
        return ResponseEntity.status(HttpStatus.CREATED).body(esteiraService.salvar(esteira));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Esteira> atualizar(@PathVariable Long id, @Valid @RequestBody Esteira dados) {
        return esteiraService.buscarPorId(id).map(e -> {
            if (dados.getNome()                  != null) e.setNome(dados.getNome());
            if (dados.getLocalizacao()           != null) e.setLocalizacao(dados.getLocalizacao());
            if (dados.getAtiva()                 != null) e.setAtiva(dados.getAtiva());
            if (dados.getLdrThreshold()          != null) e.setLdrThreshold(dados.getLdrThreshold());
            if (dados.getStopTimeoutSeconds()    != null) e.setStopTimeoutSeconds(dados.getStopTimeoutSeconds());
            if (dados.getOfflineTimeoutSeconds() != null) e.setOfflineTimeoutSeconds(dados.getOfflineTimeoutSeconds());
            if (dados.getMinimumPulses()         != null) e.setMinimumPulses(dados.getMinimumPulses());
            if (dados.getSampleIntervalMs()      != null) e.setSampleIntervalMs(dados.getSampleIntervalMs());
            return ResponseEntity.ok(esteiraService.salvar(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/manutencao")
    public ResponseEntity<?> alternarManutencao(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, Object> body) {
        boolean entrar = true;
        String motivo = "Solicitado pelo operador";
        if (body != null) {
            if (body.containsKey("entrar")) {
                entrar = Boolean.parseBoolean(body.get("entrar").toString());
            }
            if (body.containsKey("motivo")) {
                motivo = body.get("motivo").toString();
            }
        }
        try {
            Esteira atualizada = esteiraService.alternarManutencao(id, entrar, motivo);
            return ResponseEntity.ok(Map.of(
                    "sucesso", true,
                    "esteiraId", atualizada.getId(),
                    "novoEstado", atualizada.getEstado().name(),
                    "emManutencao", entrar
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Esteira> alterarStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        return esteiraService.buscarPorId(id).map(e -> {
            e.setAtiva("ATIVA".equalsIgnoreCase(body.getOrDefault("status", "")));
            return ResponseEntity.ok(esteiraService.salvar(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (esteiraService.buscarPorId(id).isEmpty()) return ResponseEntity.notFound().build();
        esteiraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}