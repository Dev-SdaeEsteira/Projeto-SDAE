package com.cba.sdae.controller.rest;

import com.cba.sdae.model.EstadoEsteira;
import com.cba.sdae.model.LeituraHistorico;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.model.TipoEventoEsteira;
import com.cba.sdae.repository.LeituraHistoricoRepository;
import com.cba.sdae.service.EsteiraService;
import com.cba.sdae.service.EventoEsteiraService;
import com.cba.sdae.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensores")
public class SensorRestController {

    private final SensorService              sensorService;
    private final EsteiraService             esteiraService;
    private final LeituraHistoricoRepository leituraRepo;
    private final EventoEsteiraService       eventoService;

    public SensorRestController(SensorService sensorService,
                                EsteiraService esteiraService,
                                LeituraHistoricoRepository leituraRepo,
                                EventoEsteiraService eventoService) {
        this.sensorService  = sensorService;
        this.esteiraService = esteiraService;
        this.leituraRepo    = leituraRepo;
        this.eventoService  = eventoService;
    }

    @GetMapping
    public List<Sensor> listar() { return sensorService.listarTodos(); }

    @GetMapping("/{id}")
    public ResponseEntity<Sensor> buscarPorId(@PathVariable Long id) {
        return sensorService.buscarPorId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/leitura")
    public ResponseEntity<Double> leitura(@PathVariable Long id) {
        return sensorService.buscarPorId(id)
                .map(s -> ResponseEntity.ok(s.getLeituraAtual()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<LeituraHistorico>> historico(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "30") int qtd) {
        if (sensorService.buscarPorId(id).isEmpty()) return ResponseEntity.notFound().build();
        int limit = Math.clamp(qtd, 1, 100);
        return ResponseEntity.ok(leituraRepo.findBySensor_IdOrderByDataHoraDesc(id, PageRequest.of(0, limit)));
    }

    @GetMapping("/leituras")
    public ResponseEntity<List<LeituraHistorico>> listarLeituras(
            @RequestParam(required = false) Long esteiraId,
            @RequestParam(required = false) Long sensorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        int limit = Math.clamp(tamanho, 1, 200);
        int page = Math.max(0, pagina);
        return ResponseEntity.ok(leituraRepo.filtrarLeituras(esteiraId, sensorId, PageRequest.of(page, limit)));
    }

    @GetMapping("/esteira/{esteiraId}")
    public ResponseEntity<List<Sensor>> porEsteira(@PathVariable Long esteiraId) {
        if (esteiraService.buscarPorId(esteiraId).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(sensorService.listarPorEsteira(esteiraId));
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody Sensor sensor,
                                   @RequestParam(required = false) Long esteiraId) {
        Long targetEsteiraId = esteiraId != null ? esteiraId : sensor.getEsteiraIdTransient();
        if (targetEsteiraId != null) {
            var esteira = esteiraService.buscarPorId(targetEsteiraId);
            if (esteira.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("erro", "Esteira não encontrada: " + targetEsteiraId));
            sensor.setEsteira(esteira.get());
        }
        if (sensor.getEsteira() == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe a esteira vinculada ao sensor."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.salvar(sensor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @Valid @RequestBody Sensor dados,
                                       @RequestParam(required = false) Long esteiraId) {
        Long targetEsteiraId = esteiraId != null ? esteiraId : dados.getEsteiraIdTransient();
        return sensorService.buscarPorId(id).map(s -> {
            if (dados.getModelo()       != null) s.setModelo(dados.getModelo());
            if (dados.getLeituraAtual() != null) s.setLeituraAtual(dados.getLeituraAtual());
            if (dados.getDescricao()    != null) s.setDescricao(dados.getDescricao());
            if (dados.getTipo()         != null) s.setTipo(dados.getTipo());
            if (dados.getStatus()       != null) s.setStatus(dados.getStatus());
            if (targetEsteiraId != null) {
                var esteira = esteiraService.buscarPorId(targetEsteiraId);
                if (esteira.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("erro", "Esteira não encontrada: " + targetEsteiraId));
                }
                s.setEsteira(esteira.get());
            }
            return ResponseEntity.ok(sensorService.salvar(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rotacionar-api-key")
    public ResponseEntity<?> rotacionarApiKey(@PathVariable Long id) {
        try {
            Sensor sensor = sensorService.rotacionarApiKey(id);
            return ResponseEntity.ok(Map.of(
                "mensagem", "Chave de API rotacionada com sucesso",
                "apiKey", sensor.getApiKey()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint oficial usado pelo ESP32 com autenticação via X-API-Key.
     * Suporta novo payload (valorLdr, laserDetectado, pulsoDetectado) e formato legado (valor).
     */
    @PostMapping("/{id}/leitura")
    public ResponseEntity<?> registrarLeitura(@PathVariable Long id,
                                              @RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                              @RequestBody(required = false) Map<String, Object> body) {
        var optSensor = sensorService.buscarPorId(id);
        if (optSensor.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Sensor s = optSensor.get();

        // Validação da API Key
        if (s.getApiKey() == null || apiKey == null ||
            !java.security.MessageDigest.isEqual(
                s.getApiKey().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Chave de API inválida ou ausente para este sensor."));
        }

        return processarPayloadLeitura(s, body);
    }

    private ResponseEntity<?> processarPayloadLeitura(Sensor s, Map<String, Object> body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Payload de leitura não fornecido."));
        }

        Integer valorLdr = null;
        Boolean laserDetectado = null;
        Boolean pulsoDetectado = null;
        Double valorLegado = null;
        LocalDateTime timestamp = null;

        // 1. Extrai valorLdr (se presente)
        if (body.containsKey("valorLdr") && body.get("valorLdr") != null) {
            Object raw = body.get("valorLdr");
            if (raw instanceof Number n) {
                valorLdr = n.intValue();
            } else {
                try {
                    valorLdr = Integer.parseInt(raw.toString().trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Extrai laserDetectado (se presente)
        if (body.containsKey("laserDetectado") && body.get("laserDetectado") != null) {
            Object raw = body.get("laserDetectado");
            if (raw instanceof Boolean b) laserDetectado = b;
            else laserDetectado = Boolean.parseBoolean(raw.toString().trim());
        }

        // 3. Extrai pulsoDetectado (se presente)
        if (body.containsKey("pulsoDetectado") && body.get("pulsoDetectado") != null) {
            Object raw = body.get("pulsoDetectado");
            if (raw instanceof Boolean b) pulsoDetectado = b;
            else pulsoDetectado = Boolean.parseBoolean(raw.toString().trim());
        }

        // 4. Extrai timestamp (se presente)
        if (body.containsKey("timestamp") && body.get("timestamp") != null) {
            try {
                timestamp = LocalDateTime.parse(body.get("timestamp").toString());
            } catch (Exception ignored) {}
        }

        // 5. Suporte legado para "valor" (%)
        if (body.containsKey("valor") && body.get("valor") != null) {
            Object raw = body.get("valor");
            if (raw instanceof Number n) {
                valorLegado = n.doubleValue();
            } else {
                try {
                    valorLegado = Double.parseDouble(raw.toString().trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // Validação mínima: precisa ter ao menos valorLdr ou valorLegado ou pulsoDetectado
        if (valorLdr == null && valorLegado == null && pulsoDetectado == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe ao menos 'valorLdr', 'pulsoDetectado' ou 'valor'."));
        }

        Map<String, Object> resultado = sensorService.processarTelemetria(
                s.getId(), valorLdr, laserDetectado, pulsoDetectado, timestamp, valorLegado);

        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (sensorService.buscarPorId(id).isEmpty()) return ResponseEntity.notFound().build();
        sensorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}