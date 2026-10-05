package com.cba.sdae.service;

import com.cba.sdae.BaseIntegrationTest;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.repository.SensorRepository;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SensorService Tests")
class SensorServiceTest extends BaseIntegrationTest {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private EsteiraRepository esteiraRepository;

    private Esteira esteira;
    private Sensor sensor;

    @BeforeEach
    void setUp() {
        sensorRepository.deleteAll();
        alertaRepository.deleteAll();
        esteiraRepository.deleteAll();

        // Create test data
        esteira = new Esteira();
        esteira.setNome("Esteira Teste");
        esteira.setLocalizacao("Prédio A - Sala 101");
        esteira.setAtiva(true);
        esteira = esteiraRepository.save(esteira);

        sensor = new Sensor();
        sensor.setModelo("ESP32-LDR");
        sensor.setEsteira(esteira);
        sensor.setLeituraAtual(30.0);
        sensor.setUltimaLeitura(LocalDateTime.now());
    }

    @Test
    @DisplayName("calcularNivel should return NORMAL for reading < 35%")
    void testCalcularNivel_Normal() {
        // Arrange
        Double leitura = 30.0; // < 35%

        // Act
        NivelRisco nivel = sensorService.calcularNivel(leitura);

        // Assert
        assertThat(nivel).isEqualTo(NivelRisco.NORMAL);
    }

    @Test
    @DisplayName("calcularNivel should return ATENCAO for reading between 35-70%")
    void testCalcularNivel_Atencao() {
        // Arrange
        Double leitura = 50.0; // Between 35-70%

        // Act
        NivelRisco nivel = sensorService.calcularNivel(leitura);

        // Assert
        assertThat(nivel).isEqualTo(NivelRisco.ATENCAO);
    }

    @Test
    @DisplayName("calcularNivel should return CRITICO for reading > 70%")
    void testCalcularNivel_Critico() {
        // Arrange
        Double leitura = 80.0; // > 70%

        // Act
        NivelRisco nivel = sensorService.calcularNivel(leitura);

        // Assert
        assertThat(nivel).isEqualTo(NivelRisco.CRITICO);
    }

    @Test
    @DisplayName("salvar should create sensor with API key")
    void testSalvar_CreatesApiKey() {
        // Act
        Sensor saved = sensorService.salvar(sensor);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getApiKey()).isNotBlank();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("salvar should fail if modelo is null")
    void testSalvar_FailsIfModeloNull() {
        // Arrange
        sensor.setModelo(null);

        // Act & Assert
        assertThatThrownBy(() -> sensorService.salvar(sensor))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("listarTodos should return all sensors")
    void testListarTodos_ReturnsAllSensors() {
        // Arrange
        sensorRepository.save(sensor);
        Sensor sensor2 = new Sensor();
        sensor2.setModelo("ESP32-DHT22");
        sensor2.setEsteira(esteira);
        sensorRepository.save(sensor2);

        // Act
        var allSensors = sensorService.listarTodos();

        // Assert
        assertThat(allSensors)
            .isNotEmpty()
            .hasSize(2)
            .extracting("modelo")
            .contains("ESP32-LDR", "ESP32-DHT22");
    }

    @Test
    @DisplayName("contarPorNivelRisco should count sensors by risk level")
    void testContarPorNivelRisco() {
        // Arrange
        sensor.setNivelRisco(NivelRisco.NORMAL);
        sensorRepository.save(sensor);

        Sensor sensor2 = new Sensor();
        sensor2.setModelo("ESP32-DHT22");
        sensor2.setEsteira(esteira);
        sensor2.setNivelRisco(NivelRisco.CRITICO);
        sensorRepository.save(sensor2);

        // Act
        long countNormal = sensorService.contarPorNivelRisco(NivelRisco.NORMAL);
        long countCritico = sensorService.contarPorNivelRisco(NivelRisco.CRITICO);

        // Assert
        assertThat(countNormal).isEqualTo(1);
        assertThat(countCritico).isEqualTo(1);
    }

    @Test
    @DisplayName("buscarMaiorLeitura should return highest reading value")
    void testBuscarMaiorLeitura() {
        // Arrange
        sensor.setLeituraAtual(30.0);
        sensorRepository.save(sensor);

        Sensor sensor2 = new Sensor();
        sensor2.setModelo("ESP32-DHT22");
        sensor2.setEsteira(esteira);
        sensor2.setLeituraAtual(85.0); // Highest
        sensorRepository.save(sensor2);

        // Act
        double maiorLeitura = sensorService.buscarMaiorLeitura();

        // Assert
        assertThat(maiorLeitura).isEqualTo(85.0);
    }
}
