package com.cba.sdae.controller;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.service.FuncionarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioController(FuncionarioService funcionarioService,
                                 PasswordEncoder passwordEncoder) {
        this.funcionarioService = funcionarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        var funcionarios = funcionarioService.listarTodos();
        model.addAttribute("funcionarios",  funcionarios);
        model.addAttribute("totalAdmin",    funcionarioService.contarPorNivelAcesso("ADMINISTRADOR"));
        model.addAttribute("totalEng",      funcionarioService.contarPorNivelAcesso("ENGENHEIRO"));
        model.addAttribute("totalOperador", funcionarioService.contarPorNivelAcesso("OPERADOR"));
        return "admin/administracao";
    }

    @GetMapping("/novo")
    public String formularioNovo(Model model) {
        model.addAttribute("funcionario", new Funcionario());
        return "cadastro/cadastrofunc";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        var func = funcionarioService.buscarPorId(id);
        if (func.isEmpty()) return "redirect:/admin";
        model.addAttribute("funcionario", func.get());
        return "cadastro/editarfunc";
    }

    // Esta rota já é protegida por hasRole("ADMINISTRADOR") em /admin/** —
    // ver SecurityConfig. Só um administrador autenticado chega aqui, então
    // a mudança de nivelAcesso/cargo de terceiros é intencional e segura.
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Funcionario funcionario) {
        try {
            if (funcionario.getId() != null) {
                // Edição: preserva a senha atual (já em hash) se o campo vier em branco;
                // se vier preenchida, re-encoda a nova senha.
                funcionarioService.buscarPorId(funcionario.getId()).ifPresent(existente -> {
                    if (funcionario.getSenha() == null || funcionario.getSenha().isBlank()) {
                        funcionario.setSenha(existente.getSenha());
                    } else {
                        funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));
                    }
                });
            } else {
                if (funcionario.getSenha() == null || funcionario.getSenha().isBlank()) {
                    return "redirect:/admin/novo?erro=true";
                }
                funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));
            }

            // Usa o cargo como nível de acesso se não informado
            if (funcionario.getNivelAcesso() == null || funcionario.getNivelAcesso().isBlank())
                funcionario.setNivelAcesso(funcionario.getCargo());

            // Matrícula sempre em maiúsculo
            if (funcionario.getMatricula() != null && !funcionario.getMatricula().isBlank())
                funcionario.setMatricula(funcionario.getMatricula().toUpperCase());

            funcionarioService.salvar(funcionario);
            return "redirect:/admin?sucesso=true";

        } catch (Exception e) {
            return funcionario.getId() != null
                    ? "redirect:/admin/editar/" + funcionario.getId() + "?erro=true"
                    : "redirect:/admin/novo?erro=true";
        }
    }

    // CORRIGIDO (S5): era @GetMapping — este é o caso mais sensível de todos,
    // já que exclui contas de funcionários (inclusive administradores).
    // O template admin/administracao.html precisa passar a usar um
    // <form method="post"> em vez de <a href="...">.
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id) {
        try {
            funcionarioService.deletar(id);
            return "redirect:/admin?sucesso=true";
        } catch (IllegalArgumentException e) {
            return "redirect:/admin?erro=true";
        }
    }
}