package com.cba.sdae.service;

import com.cba.sdae.BaseIntegrationTest;
import com.cba.sdae.model.*;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import com.cba.sdae.repository.EventoEsteiraRepository;
import com.cba.sdae.repository.LeituraHistoricoRepository;
import com.cba.sdae.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integração Real ESP32 + Laser + LDR")
class IntegracaoRealEsp32Test extends BaseIntegrationTest {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private EsteiraService esteiraService;

    @Autowired
    private EsteiraMonitorService esteiraMonitorService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private EsteiraRepository esteiraRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private EventoEsteiraRepository eventoRepository;

    @Autowired
    private LeituraHistoricoRepository leituraRepo;

    private Esteira esteira;
    private Sensor sensor;

    @BeforeEach
    void setup() {
        leituraRepo.deleteAll();
        eventoRepository.deleteAll();
        alertaRepository.deleteAll();
        sensorRepository.deleteAll();
        esteiraRepository.deleteAll();

        esteira = new Esteira("Esteira 01", "Linha de Produção Principal", true);
        esteira.setStopTimeoutSeconds(2);      // 2 segundos para o teste
        esteira.setOfflineTimeoutSeconds(2);   // 2 segundos para o teste
        esteira.setLdrThreshold(1800);
        esteira.setEstado(EstadoEsteira.STOPPED);
        esteira = esteiraRepository.save(esteira);

        sensor = new Sensor();
        sensor.setModelo("LDR-001");
        sensor.setEsteira(esteira);
        sensor.setApiKey("CHAVE-TESTE-12345");
        sensor = sensorRepository.save(sensor);
    }

    @Test
    @DisplayName("TESTE 1: ESP32 envia laserDetectado=true e pulsoDetectado=true -> Esteira = RUNNING")
    void teste1_movimentoDetectado_deveMudarParaRunning() {
        // Act: ESP32 envia telemetria com pulso
        Map<String, Object> resp = sensorService.processarTelemetria(
                sensor.getId(), 1850, true, true, LocalDateTime.now(), null);

        // Assert
        assertThat(resp.get("success")).isEqualTo(true);
        assertThat(resp.get("estadoEsteira")).isEqualTo("RUNNING");

        Esteira eAtual = esteiraRepository.findById(esteira.getId()).orElseThrow();
        assertThat(eAtual.getEstado()).isEqualTo(EstadoEsteira.RUNNING);
        assertThat(eAtual.getUltimoPulso()).isNotNull();

        // Verifica que persistiu leitura no banco
        List<LeituraHistorico> leituras = leituraRepo.findByEsteira_IdOrderByDataHoraDesc(
                esteira.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(leituras).isNotEmpty();
        assertThat(leituras.get(0).getValorLdr()).isEqualTo(1850);
        assertThat(leituras.get(0).getPulsoDetectado()).isTrue();
        assertThat(leituras.get(0).getEstadoEsteira()).isEqualTo(EstadoEsteira.RUNNING);

        // Verifica evento START
        List<EventoEsteira> eventos = eventoRepository.findByEsteira_IdOrderByDataHoraInicioDesc(
                esteira.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(eventos).isNotEmpty();
        assertThat(eventos.get(0).getTipoEvento()).isEqualTo(TipoEventoEsteira.START);
    }

    @Test
    @DisplayName("TESTE 2: ESP32 para de enviar pulsos -> Após STOP_TIMEOUT -> Esteira = STOPPED com Evento e Alerta")
    void teste2_paradaDePulsos_deveMudarParaStopped() {
        // Coloca esteira em RUNNING inicialmente
        sensorService.processarTelemetria(sensor.getId(), 1850, true, true, LocalDateTime.now(), null);

        Esteira eRodando = esteiraRepository.findById(esteira.getId()).orElseThrow();
        assertThat(eRodando.getEstado()).isEqualTo(EstadoEsteira.RUNNING);

        // Simula passagem do tempo sem novos pulsos (ultimoPulso há mais de 3 segundos)
        eRodando.setUltimoPulso(LocalDateTime.now().minusSeconds(5));
        esteiraRepository.save(eRodando);

        // Executa verificação do monitor de timeouts
        esteiraMonitorService.verificarTimeouts();

        // Assert
        Esteira eParada = esteiraRepository.findById(esteira.getId()).orElseThrow();
        assertThat(eParada.getEstado()).isEqualTo(EstadoEsteira.STOPPED);
        assertThat(eParada.getTempoParadaInicio()).isNotNull();

        // Verifica criação do evento STOP
        List<EventoEsteira> eventos = eventoRepository.findByEsteira_IdOrderByDataHoraInicioDesc(
                esteira.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(eventos).anyMatch(ev -> ev.getTipoEvento() == TipoEventoEsteira.STOP);

        // Verifica criação do Alerta Crítico no banco MySQL
        List<Alerta> alertas = alertaRepository.findByEsteira_Id(esteira.getId());
        assertThat(alertas).anyMatch(a -> a.getNivelRisco() == NivelRisco.CRITICO && a.getMensagem().contains("parada"));
    }

    @Test
    @DisplayName("TESTE 3: ESP32 volta a enviar pulsos -> Esteira = RUNNING, encerra evento anterior e registra retorno")
    void teste3_retornoDePulsos_deveRetornarParaRunning() {
        // 1. Inicia
        sensorService.processarTelemetria(sensor.getId(), 1850, true, true, LocalDateTime.now(), null);
        // 2. Para
        Esteira e = esteiraRepository.findById(esteira.getId()).orElseThrow();
        e.setUltimoPulso(LocalDateTime.now().minusSeconds(5));
        esteiraRepository.save(e);
        esteiraMonitorService.verificarTimeouts();

        assertThat(esteiraRepository.findById(esteira.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoEsteira.STOPPED);

        // 3. Volta a enviar pulsos
        sensorService.processarTelemetria(sensor.getId(), 1900, true, true, LocalDateTime.now(), null);

        // Assert: Esteira voltou para RUNNING
        Esteira eRetorno = esteiraRepository.findById(esteira.getId()).orElseThrow();
        assertThat(eRetorno.getEstado()).isEqualTo(EstadoEsteira.RUNNING);

        // Alerta informando que a esteira voltou a funcionar
        List<Alerta> alertas = alertaRepository.findByEsteira_Id(esteira.getId());
        assertThat(alertas).anyMatch(a -> a.getMensagem().contains("voltou a funcionar"));
    }

    @Test
    @DisplayName("TESTE 4: ESP32 deixa de enviar comunicação -> Após OFFLINE_TIMEOUT -> Esteira = OFFLINE e Alerta")
    void teste4_perdaDeComunicacao_deveMudarParaOffline() {
        // Estabelece contato inicial
        sensorService.processarTelemetria(sensor.getId(), 1850, true, false, LocalDateTime.now(), null);

        Esteira e = esteiraRepository.findById(esteira.getId()).orElseThrow();
        // Simula que último contato foi há 10 segundos
        e.setUltimoContato(LocalDateTime.now().minusSeconds(10));
        esteiraRepository.save(e);

        // Executa monitor
        esteiraMonitorService.verificarTimeouts();

        // Assert
        Esteira eOffline = esteiraRepository.findById(esteira.getId()).orElseThrow();
        assertThat(eOffline.getEstado()).isEqualTo(EstadoEsteira.OFFLINE);

        List<Alerta> alertas = alertaRepository.findByEsteira_Id(esteira.getId());
        assertThat(alertas).anyMatch(a -> a.getMensagem().contains("OFFLINE") || a.getMensagem().contains("comunicação"));
    }
}
