package com.cba.sdae.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String acao; // CRIAR, ATUALIZAR, EXCLUIR

    @Column(nullable = false)
    private String entidade; // ESTEIRA, SENSOR, FUNCIONARIO, ALERTA

    @Column(nullable = false)
    private Long entidadeId;

    @Column(nullable = false)
    private String usuario; // email ou matricula do usuario que executou a acao

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 1000)
    private String detalhes; // detalhes adicionais em formato texto

    // Construtores
    public Auditoria() {}

    public Auditoria(String acao, String entidade, Long entidadeId, String usuario, String detalhes) {
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.usuario = usuario;
        this.dataHora = LocalDateTime.now();
        this.detalhes = detalhes;
    }

    // Getters e Setters
    public Long getId()                   { return id; }
    public void setId(Long id)           { this.id = id; }

    public String getAcao()              { return acao; }
    public void setAcao(String acao)     { this.acao = acao; }

    public String getEntidade()          { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }

    public Long getEntidadeId()          { return entidadeId; }
    public void setEntidadeId(Long entidadeId) { this.entidadeId = entidadeId; }

    public String getUsuario()           { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDateTime getDataHora()   { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getDetalhes()          { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
}
