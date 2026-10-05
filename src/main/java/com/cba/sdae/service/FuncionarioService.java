package com.cba.sdae.service;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.repository.FuncionarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final AuditoriaService auditoriaService;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, AuditoriaService auditoriaService) {
        this.funcionarioRepository = funcionarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Funcionario> listarTodos() {
        return funcionarioRepository.findAll();
    }

    public Optional<Funcionario> buscarPorId(Long id) {
        return funcionarioRepository.findById(id);
    }

    // Busca pelo e-mail — usado para mostrar o nome do usuário logado no dashboard
    public Optional<Funcionario> buscarPorEmail(String email) {
        return funcionarioRepository.findByEmail(email);
    }

    /** Busca por e-mail ou matrícula (identificador usado no login). */
    public Optional<Funcionario> buscarPorLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        return funcionarioRepository.findByEmail(login.trim())
                .or(() -> funcionarioRepository.findByMatricula(login.trim().toUpperCase()));
    }

    public boolean existePorEmail(String email) {
        return email != null && funcionarioRepository.existsByEmail(email);
    }

    public boolean existePorMatricula(String matricula) {
        return matricula != null && funcionarioRepository.existsByMatricula(matricula.toUpperCase());
    }

    public long contarPorCargo(String cargo) {
        return funcionarioRepository.countByCargo(cargo);
    }

    public long contarPorNivelAcesso(String nivelAcesso) {
        return funcionarioRepository.countByNivelAcesso(nivelAcesso);
    }

    @Transactional
    public Funcionario salvar(Funcionario funcionario) {
        boolean isNova = funcionario.getId() == null;
        Funcionario salva = funcionarioRepository.save(funcionario);
        
        // Registrar auditoria
        String usuario = getUsuarioAtual();
        String acao = isNova ? "CRIAR" : "ATUALIZAR";
        String detalhes = String.format("Nome: %s, Email: %s, Nível: %s", 
            salva.getNome(), salva.getEmail(), salva.getNivelAcesso());
        auditoriaService.registrar(acao, "FUNCIONARIO", salva.getId(), usuario, detalhes);
        
        return salva;
    }

    @Transactional
    public void deletar(Long id) {
        buscarPorId(id).ifPresent(f -> {
            if ("ADMINISTRADOR".equalsIgnoreCase(f.getNivelAcesso())
                    && funcionarioRepository.countByNivelAcesso("ADMINISTRADOR") <= 1) {
                throw new IllegalArgumentException("Não é possível excluir o último administrador.");
            }
            
            String usuario = getUsuarioAtual();
            f.setExcluidoEm(LocalDateTime.now());
            f.setExcluidoPor(usuario);
            funcionarioRepository.save(f);
            
            // Registrar auditoria
            String detalhes = String.format("Nome: %s, Email: %s, Nível: %s", 
                f.getNome(), f.getEmail(), f.getNivelAcesso());
            auditoriaService.registrar("EXCLUIR", "FUNCIONARIO", id, usuario, detalhes);
        });
    }

    private String getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "sistema";
    }
}