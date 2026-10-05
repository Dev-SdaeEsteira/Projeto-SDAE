package com.cba.sdae.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    /**
     * Exibe a página de login.
     *
     * O Spring Security processa o POST /login automaticamente (via SecurityConfig).
     * Aqui só precisamos exibir a página e passar mensagens de erro/sucesso para o HTML.
     *
     * Parâmetros de URL que o Spring Security envia automaticamente:
     *   ?erro=true   → credenciais incorretas
     *   ?logout=true → usuário acabou de sair
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "erro",     required = false) String erro,
                        @RequestParam(value = "logout",   required = false) String logout,
                        @RequestParam(value = "cadastro", required = false) String cadastro,
                        Model model) {

        if (erro != null) {
            model.addAttribute("erro", "E-mail ou senha incorretos. Tente novamente.");
        }
        if (logout != null) {
            model.addAttribute("logout", "Você saiu do sistema com sucesso.");
        }
        if (cadastro != null) {
            model.addAttribute("logout", "Conta criada com sucesso! Faça login para continuar.");
        }

        return "login/login";
    }


}