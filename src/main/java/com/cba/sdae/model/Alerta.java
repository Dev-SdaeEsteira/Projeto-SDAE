package com.cba.sdae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mensagem;

    // Data e hora que o alerta foi gerado
    @Column(nullable = false)
    private LocalDateTime dataHora;

    // Nível de risco no momento do alerta
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco nivelRisco;

    // Muitos alertas pertencem a uma esteira
    @ManyToOne
    @JoinColumn(name = "esteira_id", nullable = false)
    @JsonIgnore
    private Esteira esteira;

    // Construtores
    public Alerta() {}

    public Alerta(String mensagem, LocalDateTime dataHora, NivelRisco nivelRisco, Esteira esteira) {
        this.mensagem = mensagem;
        this.dataHora = dataHora;
        this.nivelRisco = nivelRisco;
        this.esteira = esteira;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public NivelRisco getNivelRisco() { return nivelRisco; }
    public void setNivelRisco(NivelRisco nivelRisco) { this.nivelRisco = nivelRisco; }

    public Esteira getEsteira() { return esteira; }
    public void setEsteira(Esteira esteira) { this.esteira = esteira; }

    @JsonProperty("esteiraId")
    public Long getEsteiraId() {
        return esteira != null ? esteira.getId() : null;
    }

    @JsonProperty("esteiraNome")
    public String getEsteiraNome() {
        return esteira != null ? esteira.getNome() : null;
    }
}