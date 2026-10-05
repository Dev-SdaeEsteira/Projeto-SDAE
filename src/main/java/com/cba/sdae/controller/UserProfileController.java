package com.cba.sdae.controller;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.service.FuncionarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/perfil")
public class UserProfileController {

    private final FuncionarioService funcionarioService;
    private final PasswordEncoder passwordEncoder;

    public UserProfileController(FuncionarioService funcionarioService,
                                 PasswordEncoder passwordEncoder) {
        this.funcionarioService = funcionarioService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Exibe a página de perfil do usuário logado
     */
    @GetMapping
    public String exibir(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        var funcionario = funcionarioService.buscarPorLogin(auth.getName());

        if (funcionario.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("funcionario", funcionario.get());
        model.addAttribute("nomeUsuario", funcionario.get().getNome());
        return "perfil/perfil";
    }

    /**
     * Salva alterações na senha do usuário logado
     */
    @PostMapping("/salvar")
    public String salvar(@RequestParam Long id,
                         @RequestParam String senhaAtual,
                         @RequestParam String novaSenha,
                         @RequestParam String confirmarSenha,
                         Authentication auth) {
        
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        var funcionarioAtual = funcionarioService.buscarPorLogin(auth.getName());
        
        if (funcionarioAtual.isEmpty() || !funcionarioAtual.get().getId().equals(id)) {
            return "redirect:/perfil?erro=true";
        }

        var funcionario = funcionarioAtual.get();

        // Validar se a senha atual está correta
        if (!passwordEncoder.matches(senhaAtual, funcionario.getSenha())) {
            return "redirect:/perfil?erro=true";
        }

        // Validar se as novas senhas coincidem
        if (!novaSenha.equals(confirmarSenha) || novaSenha.isBlank() || novaSenha.length() < 6) {
            return "redirect:/perfil?erro=true";
        }

        try {
            // Atualizar a senha (com hash)
            funcionario.setSenha(passwordEncoder.encode(novaSenha));
            funcionarioService.salvar(funcionario);
            return "redirect:/perfil?sucesso=true";
        } catch (Exception e) {
            return "redirect:/perfil?erro=true";
        }
    }
}
