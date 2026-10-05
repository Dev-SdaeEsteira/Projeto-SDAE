package com.cba.sdae.security;

import com.cba.sdae.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void publicEndpoints_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/welcome"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk());
    }

    @Test
    public void adminEndpoints_shouldRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isFound()); // Redirect to login
        
        mockMvc.perform(get("/admin/esteiras"))
                .andExpect(status().isFound());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    public void adminEndpoints_shouldBlockOperatorRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void adminEndpoints_shouldAllowAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ENGENHEIRO")
    public void adminEndpoints_shouldBlockEngineerRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void apiEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/esteiras"))
                .andExpect(status().isFound()); // Redirect to login
        
        mockMvc.perform(get("/api/sensores"))
                .andExpect(status().isFound());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    public void apiReadEndpoints_shouldAllowAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/esteiras"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/sensores"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    public void apiWriteEndpoints_shouldBlockOperatorRole() throws Exception {
        mockMvc.perform(post("/api/esteiras")
                .with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Test\",\"localizacao\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENGENHEIRO")
    public void apiWriteEndpoints_shouldAllowEngineerRole() throws Exception {
        mockMvc.perform(post("/api/esteiras")
                .with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Test Esteira Completa\",\"localizacao\":\"Localização Teste Completa\"}"))
                .andExpect(status().isCreated()); // Should not be forbidden
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void apiWriteEndpoints_shouldAllowAdminRole() throws Exception {
        mockMvc.perform(post("/api/esteiras")
                .with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Test Esteira Completa\",\"localizacao\":\"Localização Teste Completa\"}"))
                .andExpect(status().isCreated()); // Should not be forbidden
    }

    @Test
    @WithMockUser(roles = "ENGENHEIRO")
    public void funcionariosApi_shouldBlockEngineerRole() throws Exception {
        mockMvc.perform(get("/api/funcionarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void funcionariosApi_shouldAllowAdminRole() throws Exception {
        mockMvc.perform(get("/api/funcionarios"))
                .andExpect(status().isOk());
    }
}
