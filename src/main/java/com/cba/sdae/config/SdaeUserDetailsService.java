package com.cba.sdae.config;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.repository.FuncionarioRepository;
import com.cba.sdae.service.LoginAttemptService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ensina o Spring Security a buscar o usuário no banco de dados.
 * Quando alguém faz POST /login com email + senha, o Spring chama este serviço.
 */
@Service
public class SdaeUserDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final LoginAttemptService loginAttemptService;

    public SdaeUserDetailsService(FuncionarioRepository funcionarioRepository,
                                   LoginAttemptService loginAttemptService) {
        this.funcionarioRepository = funcionarioRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        if (login == null || login.isBlank()) {
            throw new UsernameNotFoundException("Identificador de usuário não informado.");
        }

        String cleanLogin = login.trim();

        Funcionario f = funcionarioRepository.findByEmail(cleanLogin)
                .or(() -> funcionarioRepository.findByMatricula(cleanLogin.toUpperCase()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        String nivel = f.getNivelAcesso() != null ? f.getNivelAcesso() : "OPERADOR";
        String role  = "ROLE_" + nivel.toUpperCase();
        String username = f.getEmail() != null ? f.getEmail()
                : f.getMatricula() != null ? f.getMatricula()
                : cleanLogin;

        return new User(
                username,
                f.getSenha(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}