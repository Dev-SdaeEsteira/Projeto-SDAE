package com.cba.sdae.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "funcionarios")
@Where(clause = "excluido_em IS NULL")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @Column(unique = true)
    @Size(max = 20, message = "Matrícula deve ter no máximo 20 caracteres")
    private String matricula;

    @Column(unique = true)
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    private String cpf;

    @Column(unique = true)
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    /** OPERADOR | ENGENHEIRO | ADMINISTRADOR */
    @Size(max = 50, message = "Cargo deve ter no máximo 50 caracteres")
    private String cargo;

    /** Nome do departamento, ex: "Geral", "Manutenção" */
    @Size(max = 50, message = "Departamento deve ter no máximo 50 caracteres")
    private String departamento;

    /** MANHA | TARDE | NOITE | INTEGRAL */
    @NotBlank(message = "Turno é obrigatório")
    @Pattern(regexp = "MANHA|TARDE|NOITE|INTEGRAL", message = "Turno inválido")
    private String turno;

    /** OPERADOR | ENGENHEIRO | ADMINISTRADOR */
    @NotBlank(message = "Nível de acesso é obrigatório")
    @Pattern(regexp = "OPERADOR|ENGENHEIRO|ADMINISTRADOR", message = "Nível de acesso inválido")
    private String nivelAcesso;

    /** Hash BCrypt — aceita senha no JSON de escrita, nunca devolvida na leitura. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    // Soft-delete: data de exclusão (null = ativo)
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    // Soft-delete: quem excluiu (null = não excluído)
    @Column(name = "excluido_por")
    private String excluidoPor;

    // ── Construtores ──────────────────────────────────────────────
    public Funcionario() {}

    public Funcionario(String nome, String email, String cargo, String turno) {
        this.nome  = nome;
        this.email = email;
        this.cargo = cargo;
        this.turno = turno;
        if (cargo != null && (cargo.equals("OPERADOR") || cargo.equals("ENGENHEIRO") || cargo.equals("ADMINISTRADOR"))) {
            this.nivelAcesso = cargo;
        }
    }

    // ── Getters e Setters ─────────────────────────────────────────
    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }

    public String getNome()                  { return nome; }
    public void setNome(String nome)         { this.nome = nome; }

    public String getMatricula()             { return matricula; }
    public void setMatricula(String m)       { this.matricula = m; }

    public String getCpf()                   { return cpf; }
    public void setCpf(String cpf)           { this.cpf = cpf; }

    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }

    public String getTelefone()              { return telefone; }
    public void setTelefone(String t)        { this.telefone = t; }

    public String getCargo()                 { return cargo; }
    public void setCargo(String cargo)       { this.cargo = cargo; }

    public String getDepartamento()          { return departamento; }
    public void setDepartamento(String d)    { this.departamento = d; }

    public String getTurno()                 { return turno; }
    public void setTurno(String turno)       { this.turno = turno; }

    public String getNivelAcesso()           { return nivelAcesso; }
    public void setNivelAcesso(String n)     { this.nivelAcesso = n; }

    public String getSenha()                 { return senha; }
    public void setSenha(String senha)       { this.senha = senha; }

    public LocalDateTime getExcluidoEm()           { return excluidoEm; }
    public void setExcluidoEm(LocalDateTime excluidoEm) { this.excluidoEm = excluidoEm; }

    public String getExcluidoPor()                 { return excluidoPor; }
    public void setExcluidoPor(String excluidoPor) { this.excluidoPor = excluidoPor; }

    public boolean isExcluido() {
        return excluidoEm != null;
    }
}