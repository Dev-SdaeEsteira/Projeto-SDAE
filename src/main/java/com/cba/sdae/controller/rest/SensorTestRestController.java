package com.cba.sdae.controller.rest;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.EstadoEsteira;
import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.model.TipoEventoEsteira;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import com.cba.sdae.service.EventoEsteiraService;
import com.cba.sdae.service.SensorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller de testes e simulação para validação sem hardware físico (Req. 30).
 * Permite simular leituras e comandos: START, STOP, OFFLINE, ONLINE, LASER_LOST.
 */
@RestController
@RequestMapping("/api/test/sensores")
public class SensorTestRestController {

    private final SensorService        sensorService;
    private final EsteiraRepository    esteiraRepository;
    private final AlertaRepository     alertaRepository;
    private final EventoEsteiraService eventoService;

    public SensorTestRestController(SensorService sensorService,
                                    EsteiraRepository esteiraRepository,
                                    AlertaRepository alertaRepository,
                                    EventoEsteiraService eventoService) {
        this.sensorService     = sensorService;
        this.esteiraRepository = esteiraRepository;
        this.alertaRepository  = alertaRepository;
        this.eventoService     = eventoService;
    }

    /**
     * POST /api/test/sensores/{id}/leitura
     * Simula o envio de telemetria sem exigir o cabeçalho X-API-Key.
     */
    @PostMapping("/{id}/leitura")
    public ResponseEntity<?> simularLeitura(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, Object> body) {
        var optSensor = sensorService.buscarPorId(id);
        if (optSensor.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> payload = (body != null) ? body : Map.of();

        // Verifica se há comando direto no body
        if (payload.containsKey("comando")) {
            String comando = payload.get("comando").toString().trim().toUpperCase();
            return executarComando(id, comando);
        }

        Integer valorLdr = payload.containsKey("valorLdr") ? parseInteger(payload.get("valorLdr")) : 1800;
        Boolean laserDetectado = payload.containsKey("laserDetectado") ? parseBoolean(payload.get("laserDetectado")) : true;
        Boolean pulsoDetectado = payload.containsKey("pulsoDetectado") ? parseBoolean(payload.get("pulsoDetectado")) : true;
        Double valorLegado = payload.containsKey("valor") ? parseDouble(payload.get("valor")) : null;

        Map<String, Object> resultado = sensorService.processarTelemetria(
                id, valorLdr, laserDetectado, pulsoDetectado, LocalDateTime.now(), valorLegado);

        return ResponseEntity.ok(resultado);
    }

    /**
     * POST /api/test/sensores/{id}/comando/{comando}
     * Executa comandos específicos: START, STOP, OFFLINE, ONLINE, LASER_LOST.
     */
    @PostMapping("/{id}/comando/{comando}")
    public ResponseEntity<?> comandoDireto(@PathVariable Long id, @PathVariable String comando) {
        return executarComando(id, comando.toUpperCase());
    }

    private ResponseEntity<?> executarComando(Long sensorId, String comando) {
        var optSensor = sensorService.buscarPorId(sensorId);
        if (optSensor.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Sensor sensor = optSensor.get();
        Esteira esteira = sensor.getEsteira();
        LocalDateTime agora = LocalDateTime.now();
        EstadoEsteira anterior = esteira.getEstado();

        switch (comando) {
            case "START" -> {
                // Simula sensor enviando pulsos válidos
                return ResponseEntity.ok(sensorService.processarTelemetria(
                        sensorId, 1850, true, true, agora, null));
            }
            case "STOP" -> {
                esteira.setEstado(EstadoEsteira.STOPPED);
                esteira.setTempoParadaInicio(agora);
                esteiraRepository.save(esteira);

                eventoService.registrarTransicao(esteira, TipoEventoEsteira.STOP, anterior,
                        EstadoEsteira.STOPPED, "Simulação de parada manual acionada no teste");
                alertaRepository.save(new Alerta(
                        "ALERTA CRÍTICO: Esteira: " + esteira.getNome() + ". A esteira está parada há 15 segundos (simulação de teste).",
                        agora, NivelRisco.CRITICO, esteira));

                return ResponseEntity.ok(Map.of(
                        "sucesso", true,
                        "esteiraId", esteira.getId(),
                        "novoEstado", EstadoEsteira.STOPPED.name(),
                        "mensagem", "Comando STOP executado com sucesso."
                ));
            }
            case "OFFLINE" -> {
                esteira.setEstado(EstadoEsteira.OFFLINE);
                esteiraRepository.save(esteira);

                eventoService.registrarTransicao(esteira, TipoEventoEsteira.OFFLINE, anterior,
                        EstadoEsteira.OFFLINE, "Simulação de perda de comunicação no teste");
                alertaRepository.save(new Alerta(
                        "ALERTA CRÍTICO: Sensor " + sensor.getModelo() + " da esteira '" + esteira.getNome() + "' está sem comunicação (OFFLINE - teste).",
                        agora, NivelRisco.CRITICO, esteira));

                return ResponseEntity.ok(Map.of(
                        "sucesso", true,
                        "esteiraId", esteira.getId(),
                        "novoEstado", EstadoEsteira.OFFLINE.name(),
                        "mensagem", "Comando OFFLINE executado com sucesso."
                ));
            }
            case "ONLINE" -> {
                esteira.setEstado(EstadoEsteira.STOPPED);
                esteira.setUltimoContato(agora);
                sensor.setUltimoContato(agora);
                esteiraRepository.save(esteira);

                eventoService.registrarTransicao(esteira, TipoEventoEsteira.ONLINE, anterior,
                        EstadoEsteira.STOPPED, "Simulação de retorno de comunicação");
                alertaRepository.save(new Alerta(
                        "Comunicação restabelecida com o sensor " + sensor.getModelo() + " da esteira '" + esteira.getNome() + "'.",
                        agora, NivelRisco.NORMAL, esteira));

                return ResponseEntity.ok(Map.of(
                        "sucesso", true,
                        "esteiraId", esteira.getId(),
                        "novoEstado", EstadoEsteira.STOPPED.name(),
                        "mensagem", "Comando ONLINE executado com sucesso."
                ));
            }
            case "LASER_LOST" -> {
                sensor.setLaserDetectado(false);
                sensor.setUltimoValorLdr(50); // Valor baixo sem laser
                sensor.setUltimoContato(agora);
                sensorService.salvar(sensor);

                eventoService.registrarTransicao(esteira, TipoEventoEsteira.LASER_LOST, anterior,
                        esteira.getEstado(), "Feixe de laser interrompido ou perdido (simulação)");
                alertaRepository.save(new Alerta(
                        "ALERTA: Feixe de laser interrompido/perdido no sensor " + sensor.getModelo() + " da esteira '" + esteira.getNome() + "'.",
                        agora, NivelRisco.ATENCAO, esteira));

                return ResponseEntity.ok(Map.of(
                        "sucesso", true,
                        "laserDetectado", false,
                        "mensagem", "Comando LASER_LOST registrado com sucesso."
                ));
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "erro", "Comando desconhecido: " + comando + ". Comandos permitidos: START, STOP, OFFLINE, ONLINE, LASER_LOST."
                ));
            }
        }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString().trim()); } catch (Exception e) { return null; }
    }

    private Boolean parseBoolean(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean b) return b;
        return Boolean.parseBoolean(obj.toString().trim());
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(obj.toString().trim()); } catch (Exception e) { return null; }
    }
}
