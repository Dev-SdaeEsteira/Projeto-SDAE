package com.cba.sdae.controller;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.service.FuncionarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cadastro")
public class CadastroController {

    private final FuncionarioService funcionarioService;
    private final PasswordEncoder passwordEncoder;

    public CadastroController(FuncionarioService funcionarioService,
                              PasswordEncoder passwordEncoder) {
        this.funcionarioService = funcionarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String exibir(
            @RequestParam(required = false) String sucesso,
            @RequestParam(required = false) String erro,
            Model model) {

        if (sucesso != null)
            model.addAttribute("sucesso", "Cadastro realizado! Faça login para continuar.");
        if (erro != null)
            model.addAttribute("erro", "E-mail ou matrícula já cadastrados.");

        return "login/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(
            @RequestParam String nome,
            @RequestParam(required = false) String sobrenome,
            @RequestParam String email,
            @RequestParam String matricula,
            // CORRIGIDO (S7): "nivelAcesso" deixou de vir do formulário público.
            // O cadastro público sempre cria um OPERADOR; qualquer promoção de
            // nível de acesso passa a exigir um ADMINISTRADOR autenticado
            // usando a tela /admin (FuncionarioController), nunca o próprio
            // usuário se autodeclarando.
            @RequestParam String cargo,
            @RequestParam String turno,
            @RequestParam(required = false) String departamento,
            @RequestParam String senha) {

        if (senha == null || senha.length() < 6)
            return "redirect:/cadastro?erro=true";

        if (funcionarioService.existePorEmail(email))
            return "redirect:/cadastro?erro=true";
        if (funcionarioService.existePorMatricula(matricula))
            return "redirect:/cadastro?erro=true";

        try {
            Funcionario f = new Funcionario();
            String nomeCompleto = (sobrenome != null && !sobrenome.isBlank())
                    ? nome + " " + sobrenome : nome;

            f.setNome(nomeCompleto);
            f.setEmail(email);
            f.setMatricula(matricula.toUpperCase());
            // Papel de acesso fixo — nunca aceito do cliente no autocadastro.
            f.setNivelAcesso("OPERADOR");
            f.setCargo(cargo.toUpperCase());
            f.setTurno(turno);
            f.setDepartamento(departamento != null ? departamento : "Geral");
            // CORRIGIDO (S2): senha gravada com BCrypt.
            f.setSenha(passwordEncoder.encode(senha));

            funcionarioService.salvar(f);
            return "redirect:/login?cadastro=true";

        } catch (Exception e) {
            return "redirect:/cadastro?erro=true";
        }
    }
}