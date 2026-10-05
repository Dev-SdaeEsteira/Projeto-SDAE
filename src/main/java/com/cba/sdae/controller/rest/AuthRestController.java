package com.cba.sdae.controller.rest;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.repository.FuncionarioRepository;
import com.cba.sdae.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Autenticação via banco de dados.
 *
 * POST /api/auth/login
 * Body: { "login": "admin@sdae.com.br", "senha": "admin123" }
 * Resposta OK: { "nome": "Administrador", "nivel": "ADMINISTRADOR", "login": "admin@sdae.com.br" }
 *
 * O campo "login" pode ser e-mail ou matrícula.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final FuncionarioRepository funcionarioRepository;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthRestController(FuncionarioRepository funcionarioRepository,
                              AuthenticationManager authenticationManager,
                              LoginAttemptService loginAttemptService) {
        this.funcionarioRepository = funcionarioRepository;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String loginInput = body.getOrDefault("login", "").trim();
        String senha      = body.getOrDefault("senha", "");

        if (loginInput.isBlank() || senha.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Informe o usuário e a senha."));
        }

        // Get client IP for brute force protection
        String clientIP = getClientIP(request);
        String blockKey = clientIP + ":" + loginInput;

        // Check if blocked
        if (loginAttemptService.isBlocked(blockKey)) {
            return ResponseEntity.status(429)
                    .body(Map.of("erro", "Muitas tentativas de login. Tente novamente em 15 minutos."));
        }

        // Tenta achar por e-mail; se não encontrar, tenta por matrícula
        Optional<Funcionario> encontrado = funcionarioRepository.findByEmail(loginInput);
        if (encontrado.isEmpty()) {
            encontrado = funcionarioRepository.findByMatricula(loginInput.toUpperCase());
        }

        if (encontrado.isEmpty()) {
            loginAttemptService.loginFailed(blockKey);
            return ResponseEntity.status(401)
                    .body(Map.of("erro", "Usuário ou senha inválidos."));
        }

        Funcionario f = encontrado.get();

        // Use AuthenticationManager for authentication (consolidated approach)
        try {
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    f.getEmail() != null ? f.getEmail() : f.getMatricula(),
                    senha
                );
            
            var authentication = authenticationManager.authenticate(authToken);
            
            // 🔧 CORREÇÃO: troca o ID da sessão após autenticar (session fixation).
            request.getSession(true);
            request.changeSessionId();
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(authentication);
            SecurityContextHolder.setContext(sc);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            // Clear failed attempts on successful login
            loginAttemptService.loginSucceeded(blockKey);

            String nivel = f.getNivelAcesso() != null ? f.getNivelAcesso()
                    : f.getCargo()       != null ? f.getCargo()
                    : "OPERADOR";

            return ResponseEntity.ok(Map.of(
                    "nome",  f.getNome(),
                    "nivel", nivel,
                    "login", loginInput,
                    "id",    f.getId()
            ));
        } catch (Exception e) {
            loginAttemptService.loginFailed(blockKey);
            return ResponseEntity.status(401)
                    .body(Map.of("erro", "Usuário ou senha inválidos."));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * GET /api/auth/me — usuário da sessão Spring Security (após login por formulário).
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Não autenticado."));
        }

        String username = user.getUsername();
        Optional<Funcionario> encontrado = funcionarioRepository.findByEmail(username);
        if (encontrado.isEmpty()) {
            encontrado = funcionarioRepository.findByMatricula(username.toUpperCase());
        }

        if (encontrado.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("erro", "Não autenticado."));
        }

        Funcionario f = encontrado.get();
        String nivel = f.getNivelAcesso() != null ? f.getNivelAcesso()
                : f.getCargo() != null ? f.getCargo()
                : "OPERADOR";
        String login = f.getEmail() != null ? f.getEmail()
                : f.getMatricula() != null ? f.getMatricula()
                : username;

        return ResponseEntity.ok(Map.of(
                "nome",  f.getNome(),
                "nivel", nivel,
                "login", login,
                "id",    f.getId()
        ));
    }
}