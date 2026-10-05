package com.cba.sdae.controller;

import com.cba.sdae.BaseIntegrationTest;
import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.EstadoEsteira;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Testes de API REST para Sensores e Simulação")
class SensorApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private final String API_KEY = "CHAVE-API-SECRETA-123";

    @BeforeEach
    void setUp() {
        leituraRepo.deleteAll();
        eventoRepository.deleteAll();
        alertaRepository.deleteAll();
        sensorRepository.deleteAll();
        esteiraRepository.deleteAll();

        esteira = new Esteira("Esteira Teste API", "Setor Teste", true);
        esteira.setEstado(EstadoEsteira.STOPPED);
        esteira = esteiraRepository.save(esteira);

        sensor = new Sensor();
        sensor.setModelo("ESP32-LASER-LDR");
        sensor.setEsteira(esteira);
        sensor.setApiKey(API_KEY);
        sensor = sensorRepository.save(sensor);
    }

    @Test
    @DisplayName("POST /api/sensores/{id}/leitura com API Key correta deve retornar 200 e JSON de resposta")
    void registrarLeitura_apiKeyCorreta_deveRetornar200() throws Exception {
        String payload = """
            {
                "valorLdr": 1820,
                "laserDetectado": true,
                "pulsoDetectado": true
            }
        """;

        mockMvc.perform(post("/api/sensores/" + sensor.getId() + "/leitura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", API_KEY)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valorLdr").value(1820))
                .andExpect(jsonPath("$.laserDetectado").value(true))
                .andExpect(jsonPath("$.pulsoDetectado").value(true))
                .andExpect(jsonPath("$.estadoEsteira").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/sensores/{id}/leitura com API Key incorreta deve retornar 401")
    void registrarLeitura_apiKeyIncorreta_deveRetornar401() throws Exception {
        String payload = """
            {
                "valorLdr": 1820,
                "laserDetectado": true,
                "pulsoDetectado": true
            }
        """;

        mockMvc.perform(post("/api/sensores/" + sensor.getId() + "/leitura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", "CHAVE-ERRADA")
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/test/sensores/{id}/leitura sem API key deve ser permitido para simulação")
    void simularLeitura_semApiKey_deveRetornar200() throws Exception {
        String payload = """
            {
                "valorLdr": 1950,
                "laserDetectado": true,
                "pulsoDetectado": true
            }
        """;

        mockMvc.perform(post("/api/test/sensores/" + sensor.getId() + "/leitura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.estadoEsteira").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/test/sensores/{id}/comando/STOP deve alterar estado para STOPPED")
    void simularComando_stop_deveAlterarParaStopped() throws Exception {
        mockMvc.perform(post("/api/test/sensores/" + sensor.getId() + "/comando/STOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.novoEstado").value("STOPPED"));
    }

    @Test
    @WithMockUser(username = "operador", roles = "OPERADOR")
    @DisplayName("GET /api/dashboard deve retornar contadores e dados reais")
    void getDashboard_deveRetornarKpis() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esteirasFuncionando").exists())
                .andExpect(jsonPath("$.esteirasParadas").exists())
                .andExpect(jsonPath("$.esteirasOffline").exists())
                .andExpect(jsonPath("$.esteiras").isArray());
    }
}
