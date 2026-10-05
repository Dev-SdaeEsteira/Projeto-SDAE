package com.cba.sdae.service;

import com.cba.sdae.model.*;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import com.cba.sdae.repository.LeituraHistoricoRepository;
import com.cba.sdae.repository.SensorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SensorService {

    private final SensorRepository           sensorRepository;
    private final EsteiraRepository          esteiraRepository;
    private final AlertaRepository           alertaRepository;
    private final LeituraHistoricoRepository leituraHistoricoRepository;
    private final AuditoriaService           auditoriaService;
    private final EventoEsteiraService       eventoService;

    // Cache em memória para debounce de alertas por sensor
    private final Map<Long, LocalDateTime> ultimoAlertaPorSensor = new ConcurrentHashMap<>();
    private final Map<Long, NivelRisco>    ultimoNivelPorSensor  = new ConcurrentHashMap<>();

    public SensorService(SensorRepository sensorRepository,
                         EsteiraRepository esteiraRepository,
                         AlertaRepository alertaRepository,
                         LeituraHistoricoRepository leituraHistoricoRepository,
                         AuditoriaService auditoriaService,
                         EventoEsteiraService eventoService) {
        this.sensorRepository           = sensorRepository;
        this.esteiraRepository          = esteiraRepository;
        this.alertaRepository           = alertaRepository;
        this.leituraHistoricoRepository = leituraHistoricoRepository;
        this.auditoriaService           = auditoriaService;
        this.eventoService              = eventoService;
    }

    public List<Sensor>     listarTodos()                      { return sensorRepository.findAllWithEsteira(); }
    public Optional<Sensor> buscarPorId(Long id)               { return sensorRepository.findById(id); }
    public List<Sensor>     listarPorEsteira(Long esteiraId)   { return sensorRepository.findByEsteira_Id(esteiraId); }

    @Transactional
    public Sensor rotacionarApiKey(Long id) {
        return sensorRepository.findById(id).map(sensor -> {
            String usuario = getUsuarioAtual();
            String apiKeyAntiga = sensor.getApiKey();
            
            // Gerar nova chave
            sensor.setApiKey(java.util.UUID.randomUUID().toString());
            Sensor salvo = sensorRepository.save(sensor);
            
            // Registrar auditoria
            String detalhes = String.format("Modelo: %s, Chave antiga: %s, Nova chave: %s", 
                sensor.getModelo(), 
                apiKeyAntiga != null && apiKeyAntiga.length() >= 8 ? apiKeyAntiga.substring(0, 8) + "..." : "N/A", 
                salvo.getApiKey().substring(0, 8) + "...");
            auditoriaService.registrar("ROTACIONAR_API_KEY", "SENSOR", id, usuario, detalhes);
            
            return salvo;
        }).orElseThrow(() -> new IllegalArgumentException("Sensor não encontrado: " + id));
    }

    public List<LeituraHistorico> listarHistorico(Long sensorId, int qtd) {
        int limit = Math.clamp(qtd, 1, 100);
        return leituraHistoricoRepository.findBySensor_IdOrderByDataHoraDesc(
                sensorId, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    public List<Sensor>     listarPorNivelRisco(NivelRisco n)  { return sensorRepository.findByNivelRisco(n); }
    public long             contarTotal()                      { return sensorRepository.count(); }
    public long             contarPorNivelRisco(NivelRisco n)  { return sensorRepository.countByNivelRisco(n); }
    public double           buscarMaiorLeitura()               { Double max = sensorRepository.findMaxLeituraAtual(); return max != null ? max : 0.0; }
    
    @Transactional
    public void deletar(Long id) { 
        sensorRepository.findById(id).ifPresent(sensor -> {
            String usuario = getUsuarioAtual();
            sensor.setExcluidoEm(LocalDateTime.now());
            sensor.setExcluidoPor(usuario);
            sensorRepository.save(sensor);
            
            // Limpar cache de alertas
            ultimoAlertaPorSensor.remove(id);
            ultimoNivelPorSensor.remove(id);
            
            // Registrar auditoria
            String detalhes = String.format("Modelo: %s, Esteira: %s", 
                sensor.getModelo(), sensor.getEsteira() != null ? sensor.getEsteira().getNome() : "N/A");
            auditoriaService.registrar("EXCLUIR", "SENSOR", id, usuario, detalhes);
        });
    }

    @Transactional
    public Sensor salvar(Sensor sensor) {
        if (sensor == null || sensor.getModelo() == null || sensor.getModelo().isBlank()) {
            throw new IllegalArgumentException("O modelo do sensor é obrigatório.");
        }
        double leitura = sensor.getLeituraAtual() != null ? sensor.getLeituraAtual() : 0;
        NivelRisco nivel = calcularNivel(leitura);
        sensor.setNivelRisco(nivel);

        // Preserva chave de API e data da última leitura se for edição e não vierem no payload
        if (sensor.getId() != null &&
                (sensor.getApiKey() == null || sensor.getApiKey().isBlank() || sensor.getUltimaLeitura() == null)) {
            sensorRepository.findById(sensor.getId()).ifPresent(existente -> {
                if (sensor.getApiKey() == null || sensor.getApiKey().isBlank()) {
                    sensor.setApiKey(existente.getApiKey());
                }
                if (sensor.getUltimaLeitura() == null) {
                    sensor.setUltimaLeitura(existente.getUltimaLeitura());
                }
            });
        }
        if (sensor.getApiKey() == null || sensor.getApiKey().isBlank()) {
            sensor.setApiKey(java.util.UUID.randomUUID().toString());
        }

        boolean isNova = sensor.getId() == null;
        Sensor salvo = sensorRepository.save(sensor);

        // Registrar auditoria
        String usuario = getUsuarioAtual();
        String acao = isNova ? "CRIAR" : "ATUALIZAR";
        String detalhes = String.format("Modelo: %s, Leitura: %.1f%%, Nível: %s", 
            salvo.getModelo(), salvo.getLeituraAtual(), salvo.getNivelRisco());
        auditoriaService.registrar(acao, "SENSOR", salvo.getId(), usuario, detalhes);

        if (nivel != NivelRisco.NORMAL) {
            verificarEDispararAlerta(salvo, nivel, leitura);
        } else if (salvo.getId() != null) {
            ultimoNivelPorSensor.put(salvo.getId(), NivelRisco.NORMAL);
        }

        return salvo;
    }

    /**
     * Processa a telemetria enviada pelo ESP32 (ou simulador de teste)
     * e orquestra a lógica de detecção de movimento/parada, eventos e alertas.
     */
    @Transactional
    public Map<String, Object> processarTelemetria(Long sensorId, Integer valorLdr,
                                                   Boolean laserDetectado, Boolean pulsoDetectado,
                                                   LocalDateTime timestamp, Double valorLegado) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new IllegalArgumentException("Sensor não encontrado: " + sensorId));

        Esteira esteira = sensor.getEsteira();
        if (esteira == null) {
            throw new IllegalStateException("Sensor " + sensorId + " não está vinculado a nenhuma esteira.");
        }

        LocalDateTime agora = timestamp != null ? timestamp : LocalDateTime.now();

        // 1. Determina valor LDR e se laser foi detectado
        int ldr = valorLdr != null ? valorLdr : (valorLegado != null ? (int) (valorLegado * 40.95) : (sensor.getUltimoValorLdr() != null ? sensor.getUltimoValorLdr() : 0));
        boolean laser = laserDetectado != null ? laserDetectado : (ldr >= (esteira.getLdrThreshold() != null ? esteira.getLdrThreshold() : 1800));
        boolean pulso = pulsoDetectado != null && pulsoDetectado;

        // Porcentagem calculada
        double pct;
        if (valorLegado != null) {
            pct = Math.clamp(valorLegado, 0.0, 100.0);
        } else {
            pct = Math.clamp((ldr / 4095.0) * 100.0, 0.0, 100.0);
        }

        NivelRisco nivelRisco = calcularNivel(pct);

        // 2. Atualiza dados do sensor
        sensor.setUltimoContato(agora);
        sensor.setUltimaLeitura(agora);
        sensor.setUltimoValorLdr(ldr);
        sensor.setLaserDetectado(laser);
        sensor.setLeituraAtual(pct);
        sensor.setNivelRisco(nivelRisco);

        if (pulso) {
            sensor.setUltimoPulso(agora);
            sensor.setQuantidadePulsos(sensor.getQuantidadePulsos() + 1);
            esteira.setUltimoPulso(agora);
        }

        // 3. Atualiza dados da esteira e avalia transições de estado
        esteira.setUltimoContato(agora);
        EstadoEsteira estadoAnterior = esteira.getEstado();

        // Se esteira estiver em manutenção, preserva o estado MAINTENANCE
        if (estadoAnterior != EstadoEsteira.MAINTENANCE) {
            if (estadoAnterior == EstadoEsteira.OFFLINE) {
                // Comunicação restabelecida (ONLINE)
                eventoService.registrarTransicao(esteira, TipoEventoEsteira.ONLINE, EstadoEsteira.OFFLINE,
                        pulso ? EstadoEsteira.RUNNING : EstadoEsteira.STOPPED,
                        "Comunicação restabelecida com o sensor " + sensor.getModelo());
                
                if (pulso) {
                    esteira.setEstado(EstadoEsteira.RUNNING);
                    esteira.setTempoFuncionamentoInicio(agora);
                    esteira.setTempoParadaInicio(null);
                    eventoService.registrarTransicao(esteira, TipoEventoEsteira.START, EstadoEsteira.STOPPED,
                            EstadoEsteira.RUNNING, "Movimento detectado no retorno da comunicação");
                    alertaRepository.save(new Alerta("A esteira '" + esteira.getNome() + "' voltou a funcionar.",
                            agora, NivelRisco.NORMAL, esteira));
                } else {
                    esteira.setEstado(EstadoEsteira.STOPPED);
                    esteira.setTempoParadaInicio(agora);
                    alertaRepository.save(new Alerta("Comunicação restabelecida com a esteira '" + esteira.getNome() + "'. Esteira aguardando pulsos.",
                            agora, NivelRisco.NORMAL, esteira));
                }
            } else if (pulso) {
                // Pulso detectado
                if (estadoAnterior != EstadoEsteira.RUNNING) {
                    // Transição para RUNNING!
                    esteira.setEstado(EstadoEsteira.RUNNING);
                    esteira.setTempoFuncionamentoInicio(agora);
                    esteira.setTempoParadaInicio(null);

                    eventoService.registrarTransicao(esteira, TipoEventoEsteira.START, estadoAnterior,
                            EstadoEsteira.RUNNING, "Pulsos válidos detectados pelo sensor " + sensor.getModelo());

                    // Alerta de retorno (Req. 15)
                    alertaRepository.save(new Alerta("A esteira '" + esteira.getNome() + "' voltou a funcionar.",
                            agora, NivelRisco.NORMAL, esteira));
                }
            }
        }

        // 4. Persiste LeituraHistorico no banco
        LeituraHistorico historico = new LeituraHistorico(
                sensor, esteira, ldr, laser, pulso, esteira.getEstado(), pct, nivelRisco, agora
        );
        leituraHistoricoRepository.save(historico);

        // Salva sensor e esteira
        sensorRepository.save(sensor);
        esteiraRepository.save(esteira);

        // 5. Retorna mapa com formato da resposta da API (Req. 10)
        return Map.of(
                "success", true,
                "sensorId", sensor.getId(),
                "valorLdr", ldr,
                "laserDetectado", laser,
                "pulsoDetectado", pulso,
                "estadoEsteira", esteira.getEstado().name(),
                "nivelRisco", nivelRisco.name()
        );
    }

    // Calcula o nível de risco pela leitura (%)
    public NivelRisco calcularNivel(double leitura) {
        if (leitura >= 70) return NivelRisco.CRITICO;
        if (leitura >= 35) return NivelRisco.ATENCAO;
        return NivelRisco.NORMAL;
    }

    // Gera alerta inteligente (apenas em transições de estado ou a cada 15 min de insistência)
    private void verificarEDispararAlerta(Sensor sensor, NivelRisco nivel, double leitura) {
        if (sensor.getEsteira() == null || sensor.getId() == null) return;

        Long sId = sensor.getId();
        NivelRisco nivelAnterior = ultimoNivelPorSensor.get(sId);
        LocalDateTime ultimoAlerta = ultimoAlertaPorSensor.get(sId);
        LocalDateTime agora = LocalDateTime.now();

        boolean mudouNivel = (nivelAnterior == null || nivelAnterior != nivel);
        boolean tempoExpirou = (ultimoAlerta == null || Duration.between(ultimoAlerta, agora).toMinutes() >= 15);

        if (mudouNivel || tempoExpirou) {
            String mensagem = nivel == NivelRisco.CRITICO
                    ? String.format("ALERTA CRÍTICO: Sensor %s na esteira '%s' com risco elevado — leitura: %.1f%%",
                    sensor.getModelo(), sensor.getEsteira().getNome(), leitura)
                    : String.format("Sensor %s na esteira '%s' em nível de ATENÇÃO — leitura: %.1f%%",
                    sensor.getModelo(), sensor.getEsteira().getNome(), leitura);

            alertaRepository.save(new Alerta(mensagem, agora, nivel, sensor.getEsteira()));
            ultimoAlertaPorSensor.put(sId, agora);
            ultimoNivelPorSensor.put(sId, nivel);
        }
    }

    private String getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "sistema";
    }
}