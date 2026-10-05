package com.cba.sdae.service;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class BusinessLogicTest {

    @Autowired
    private FuncionarioService funcionarioService;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private SensorService sensorService;

    @Test
    @Transactional
    public void shouldNotDeleteLastAdministrator() {
        // Find the existing single administrator from DataSeeder
        Funcionario singleAdmin = funcionarioRepository.findAll().stream()
                .filter(f -> "ADMINISTRADOR".equals(f.getNivelAcesso()))
                .findFirst()
                .orElseGet(() -> {
                    Funcionario a = new Funcionario("Admin", "admin_single@test.com", "ADMINISTRADOR", "MANHA");
                    a.setSenha("password123");
                    return funcionarioRepository.save(a);
                });
        
        // Try to delete the only administrator
        assertThatThrownBy(() -> funcionarioService.deletar(singleAdmin.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é possível excluir o último administrador");
        
        // Verify admin still exists (not soft-deleted)
        assertThat(funcionarioRepository.findById(singleAdmin.getId())).isPresent();
    }

    @Test
    @Transactional
    public void shouldAllowDeleteWhenMultipleAdministratorsExist() {
        // Create an additional administrator
        Funcionario admin1 = new Funcionario("Admin 1", "admin1_extra@test.com", "ADMINISTRADOR", "MANHA");
        admin1.setSenha("password123");
        Funcionario savedAdmin1 = funcionarioService.salvar(admin1);
        
        long countBefore = funcionarioRepository.countByNivelAcesso("ADMINISTRADOR");
        assertThat(countBefore).isGreaterThan(1);
        
        // Should be able to delete one of them
        funcionarioService.deletar(savedAdmin1.getId());
        
        long countAfter = funcionarioRepository.countByNivelAcesso("ADMINISTRADOR");
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    @Test
    public void shouldCalculateRiskLevelCorrectly() {
        // Test risk levels
        assertThat(sensorService.calcularNivel(0.0)).isEqualTo(com.cba.sdae.model.NivelRisco.NORMAL);
        assertThat(sensorService.calcularNivel(20.0)).isEqualTo(com.cba.sdae.model.NivelRisco.NORMAL);
        assertThat(sensorService.calcularNivel(35.0)).isEqualTo(com.cba.sdae.model.NivelRisco.ATENCAO);
        assertThat(sensorService.calcularNivel(50.0)).isEqualTo(com.cba.sdae.model.NivelRisco.ATENCAO);
        assertThat(sensorService.calcularNivel(70.0)).isEqualTo(com.cba.sdae.model.NivelRisco.CRITICO);
        assertThat(sensorService.calcularNivel(100.0)).isEqualTo(com.cba.sdae.model.NivelRisco.CRITICO);
    }
}
