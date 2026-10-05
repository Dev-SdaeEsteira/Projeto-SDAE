package com.cba.sdae.controller.rest;

import com.cba.sdae.model.Funcionario;
import com.cba.sdae.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Protegido por hasRole("ADMINISTRADOR") em /api/funcionarios/** — ver SecurityConfig.
@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioRestController {

    private final FuncionarioService funcionarioService;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioRestController(FuncionarioService funcionarioService,
                                     PasswordEncoder passwordEncoder) {
        this.funcionarioService = funcionarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Funcionario> listar() { return funcionarioService.listarTodos(); }

    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscarPorId(@PathVariable Long id) {
        return funcionarioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody Funcionario funcionario) {
        if (funcionario.getEmail() != null && funcionarioService.existePorEmail(funcionario.getEmail()))
            return ResponseEntity.badRequest().body(Map.of("erro", "E-mail já cadastrado."));

        if (funcionario.getMatricula() != null && funcionarioService.existePorMatricula(funcionario.getMatricula()))
            return ResponseEntity.badRequest().body(Map.of("erro", "Matrícula já cadastrada."));

        // CORRIGIDO (S2): senha sempre gravada com hash, nunca em texto plano.
        funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));

        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.salvar(funcionario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody Funcionario dados) {
        return funcionarioService.buscarPorId(id).map(f -> {
            f.setNome(dados.getNome());
            f.setCargo(dados.getCargo());
            f.setDepartamento(dados.getDepartamento());
            f.setTurno(dados.getTurno());
            f.setNivelAcesso(dados.getNivelAcesso());
            if (dados.getEmail()     != null) f.setEmail(dados.getEmail());
            if (dados.getTelefone()  != null) f.setTelefone(dados.getTelefone());
            if (dados.getMatricula() != null) f.setMatricula(dados.getMatricula());
            // CORRIGIDO (S2): nova senha (se enviada) é encodada; senha em branco preserva a atual.
            if (dados.getSenha() != null && !dados.getSenha().isBlank())
                f.setSenha(passwordEncoder.encode(dados.getSenha()));
            return ResponseEntity.ok(funcionarioService.salvar(f));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (funcionarioService.buscarPorId(id).isEmpty()) return ResponseEntity.notFound().build();
        funcionarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}