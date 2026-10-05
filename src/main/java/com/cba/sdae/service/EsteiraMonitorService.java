package com.cba.sdae.service;

import com.cba.sdae.model.*;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import com.cba.sdae.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EsteiraMonitorService {

    private static final Logger log = LoggerFactory.getLogger(EsteiraMonitorService.class);

    private final EsteiraRepository    esteiraRepository;
    private final SensorRepository     sensorRepository;
    private final AlertaRepository      alertaRepository;
    private final EventoEsteiraService eventoService;

    public EsteiraMonitorService(EsteiraRepository esteiraRepository,
                                 SensorRepository sensorRepository,
                                 AlertaRepository alertaRepository,
                                 EventoEsteiraService eventoService) {
        this.esteiraRepository = esteiraRepository;
        this.sensorRepository  = sensorRepository;
        this.alertaRepository  = alertaRepository;
        this.eventoService     = eventoService;
    }

    /**
     * Roda a cada 2 segundos para verificar timeouts de parada e offline.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void verificarTimeouts() {
        List<Esteira> esteiras = esteiraRepository.findAll();
        LocalDateTime agora = LocalDateTime.now();

        for (Esteira esteira : esteiras) {
            if (esteira.isExcluido() || !esteira.getAtiva()) {
                continue;
            }

            EstadoEsteira estadoAtual = esteira.getEstado();
            if (estadoAtual == EstadoEsteira.MAINTENANCE) {
                // Em manutenção manual: timeouts não alteram para STOPPED ou OFFLINE automaticamente
                continue;
            }

            int stopTimeout = esteira.getStopTimeoutSeconds() != null ? esteira.getStopTimeoutSeconds() : 15;
            int offlineTimeout = esteira.getOfflineTimeoutSeconds() != null ? esteira.getOfflineTimeoutSeconds() : 15;

            // 1. Verifica se ficou OFFLINE (sem qualquer comunicação do sensor)
            if (esteira.getUltimoContato() != null) {
                long segsSemContato = Duration.between(esteira.getUltimoContato(), agora).getSeconds();
                if (segsSemContato > offlineTimeout && estadoAtual != EstadoEsteira.OFFLINE) {
                    log.warn("Esteira {} [ID {}] sem comunicação há {}s. Marcando como OFFLINE.",
                            esteira.getNome(), esteira.getId(), segsSemContato);

                    esteira.setEstado(EstadoEsteira.OFFLINE);
                    eventoService.registrarTransicao(esteira, TipoEventoEsteira.OFFLINE, estadoAtual,
                            EstadoEsteira.OFFLINE, "Sensor offline por mais de " + segsSemContato + "s");

                    alertaRepository.save(new Alerta(
                            "ALERTA CRÍTICO: A esteira '" + esteira.getNome() + "' está sem comunicação há " + segsSemContato + " segundos (OFFLINE).",
                            agora, NivelRisco.CRITICO, esteira));

                    esteiraRepository.save(esteira);
                    continue;
                }
            }

            // 2. Se a esteira está RUNNING, verifica se parou de receber pulsos (STOP_TIMEOUT)
            if (estadoAtual == EstadoEsteira.RUNNING) {
                LocalDateTime refPulso = esteira.getUltimoPulso() != null ? esteira.getUltimoPulso() : esteira.getTempoFuncionamentoInicio();
                if (refPulso != null) {
                    long segsSemPulso = Duration.between(refPulso, agora).getSeconds();
                    if (segsSemPulso >= stopTimeout) {
                        log.warn("Esteira {} [ID {}] sem pulsos há {}s (timeout: {}s). Marcando como STOPPED.",
                                esteira.getNome(), esteira.getId(), segsSemPulso, stopTimeout);

                        esteira.setEstado(EstadoEsteira.STOPPED);
                        esteira.setTempoParadaInicio(agora);

                        eventoService.registrarTransicao(esteira, TipoEventoEsteira.STOP, EstadoEsteira.RUNNING,
                                EstadoEsteira.STOPPED, "Pulsos cessaram por mais de " + segsSemPulso + "s");

                        alertaRepository.save(new Alerta(
                                "ALERTA CRÍTICO: Esteira: " + esteira.getNome() + ". A esteira está parada há " + segsSemPulso + " segundos.",
                                agora, NivelRisco.CRITICO, esteira));

                        esteiraRepository.save(esteira);
                    }
                }
            }
        }
    }
}
