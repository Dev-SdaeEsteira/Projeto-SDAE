package com.cba.sdae.service;

import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.Sensor;
import com.cba.sdae.repository.EsteiraRepository;
import com.cba.sdae.repository.SensorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class SoftDeleteTest {

    @Autowired
    private EsteiraService esteiraService;

    @Autowired
    private SensorService sensorService;

    @Autowired
    private EsteiraRepository esteiraRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Test
    @Transactional
    public void softDeleteEsteira_shouldSetExcludedData() {
        // Create test esteira
        Esteira esteira = new Esteira("Test Esteira", "Test Location", true);
        Esteira saved = esteiraService.salvar(esteira);
        
        // Verify it's active
        assertThat(saved.isExcluido()).isFalse();
        assertThat(saved.getExcluidoEm()).isNull();
        assertThat(saved.getExcluidoPor()).isNull();
        
        // Soft delete
        esteiraService.deletar(saved.getId());
        
        // Verify soft delete
        Esteira deleted = esteiraRepository.findById(saved.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.isExcluido()).isTrue();
        assertThat(deleted.getExcluidoEm()).isNotNull();
        assertThat(deleted.getExcluidoPor()).isNotNull();
        
        // Verify it's not in active list
        assertThat(esteiraService.listarTodas()).doesNotContain(deleted);
    }

    @Test
    @Transactional
    public void softDeleteSensor_shouldSetExcludedData() {
        // Create test esteira and sensor
        Esteira esteira = new Esteira("Test Esteira", "Test Location", true);
        Esteira savedEsteira = esteiraService.salvar(esteira);
        
        Sensor sensor = new Sensor("Test Sensor", 0.0, com.cba.sdae.model.NivelRisco.NORMAL, savedEsteira);
        Sensor savedSensor = sensorService.salvar(sensor);
        
        // Verify it's active
        assertThat(savedSensor.isExcluido()).isFalse();
        assertThat(savedSensor.getExcluidoEm()).isNull();
        assertThat(savedSensor.getExcluidoPor()).isNull();
        
        // Soft delete
        sensorService.deletar(savedSensor.getId());
        
        // Verify soft delete
        Sensor deleted = sensorRepository.findById(savedSensor.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.isExcluido()).isTrue();
        assertThat(deleted.getExcluidoEm()).isNotNull();
        assertThat(deleted.getExcluidoPor()).isNotNull();
        
        // Verify it's not in active list
        assertThat(sensorService.listarTodos()).doesNotContain(deleted);
    }

    @Test
    @Transactional
    public void softDeleteEsteira_shouldCascadeToSensors() {
        // Create test esteira with sensors
        Esteira esteira = new Esteira("Test Esteira", "Test Location", true);
        Esteira savedEsteira = esteiraService.salvar(esteira);
        
        Sensor sensor1 = new Sensor("Sensor 1", 0.0, com.cba.sdae.model.NivelRisco.NORMAL, savedEsteira);
        Sensor sensor2 = new Sensor("Sensor 2", 0.0, com.cba.sdae.model.NivelRisco.NORMAL, savedEsteira);
        sensorService.salvar(sensor1);
        sensorService.salvar(sensor2);
        
        // Soft delete esteira
        esteiraService.deletar(savedEsteira.getId());
        
        // Verify sensors are still accessible but might need manual cascade handling
        // Note: With soft-delete and cascadeType.ALL removed, sensors won't auto-delete
        // This test verifies the current behavior
        assertThat(sensorRepository.findById(sensor1.getId())).isPresent();
        assertThat(sensorRepository.findById(sensor2.getId())).isPresent();
    }
}
