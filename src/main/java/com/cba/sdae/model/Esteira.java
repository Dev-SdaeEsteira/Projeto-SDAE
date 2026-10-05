package com.cba.sdae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "esteiras")
@Where(clause = "excluido_em IS NULL")
public class Esteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Nome da esteira é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @Column(nullable = false)
    @NotBlank(message = "Localização é obrigatória")
    @Size(min = 3, max = 100, message = "Localização deve ter entre 3 e 100 caracteres")
    private String localizacao;

    // Padrão true — esteira começa ativa ao ser cadastrada
    @Column(nullable = false)
    private Boolean ativa = true;

    // Estado da esteira: RUNNING, STOPPED, OFFLINE, MAINTENANCE, ERROR
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEsteira estado = EstadoEsteira.STOPPED;

    // Configurações por esteira (armazenadas no banco)
    @Column(name = "ldr_threshold", nullable = false)
    private Integer ldrThreshold = 1800;

    @Column(name = "stop_timeout_seconds", nullable = false)
    private Integer stopTimeoutSeconds = 15;

    @Column(name = "offline_timeout_seconds", nullable = false)
    private Integer offlineTimeoutSeconds = 15;

    @Column(name = "minimum_pulses", nullable = false)
    private Integer minimumPulses = 2;

    @Column(name = "sample_interval_ms", nullable = false)
    private Integer sampleIntervalMs = 50;

    // Telemetria em tempo real
    @Column(name = "ultimo_contato")
    private LocalDateTime ultimoContato;

    @Column(name = "ultimo_pulso")
    private LocalDateTime ultimoPulso;

    @Column(name = "tempo_parada_inicio")
    private LocalDateTime tempoParadaInicio;

    @Column(name = "tempo_funcionamento_inicio")
    private LocalDateTime tempoFuncionamentoInicio;

    // Soft-delete: data de exclusão (null = ativo)
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    // Soft-delete: quem excluiu (null = não excluído)
    @Column(name = "excluido_por")
    private String excluidoPor;

    // Uma esteira tem muitos sensores
    @OneToMany(mappedBy = "esteira")
    @JsonIgnore
    private List<Sensor> sensores = new ArrayList<>();

    // Uma esteira tem muitos alertas
    @OneToMany(mappedBy = "esteira")
    @JsonIgnore
    private List<Alerta> alertas = new ArrayList<>();

    // Uma esteira tem muitos eventos
    @OneToMany(mappedBy = "esteira", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<EventoEsteira> eventos = new ArrayList<>();

    // Construtores
    public Esteira() {}

    public Esteira(String nome, String localizacao, Boolean ativa) {
        this.nome        = nome;
        this.localizacao = localizacao;
        this.ativa       = ativa;
        this.estado      = EstadoEsteira.STOPPED;
    }

    // Getters e Setters
    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }

    public String getNome()                        { return nome; }
    public void setNome(String nome)               { this.nome = nome; }

    public String getLocalizacao()                 { return localizacao; }
    public void setLocalizacao(String loc)         { this.localizacao = loc; }

    public Boolean getAtiva()                      { return ativa; }
    public void setAtiva(Boolean ativa)            { this.ativa = ativa != null && ativa; }

    public EstadoEsteira getEstado()               { return estado; }
    public void setEstado(EstadoEsteira estado)    { this.estado = estado != null ? estado : EstadoEsteira.STOPPED; }

    public Integer getLdrThreshold()               { return ldrThreshold; }
    public void setLdrThreshold(Integer ldr)       { this.ldrThreshold = ldr != null ? ldr : 1800; }

    public Integer getStopTimeoutSeconds()         { return stopTimeoutSeconds; }
    public void setStopTimeoutSeconds(Integer s)   { this.stopTimeoutSeconds = s != null ? s : 15; }

    public Integer getOfflineTimeoutSeconds()      { return offlineTimeoutSeconds; }
    public void setOfflineTimeoutSeconds(Integer o){ this.offlineTimeoutSeconds = o != null ? o : 15; }

    public Integer getMinimumPulses()              { return minimumPulses; }
    public void setMinimumPulses(Integer m)        { this.minimumPulses = m != null ? m : 2; }

    public Integer getSampleIntervalMs()           { return sampleIntervalMs; }
    public void setSampleIntervalMs(Integer s)     { this.sampleIntervalMs = s != null ? s : 50; }

    public LocalDateTime getUltimoContato()        { return ultimoContato; }
    public void setUltimoContato(LocalDateTime u)  { this.ultimoContato = u; }

    public LocalDateTime getUltimoPulso()          { return ultimoPulso; }
    public void setUltimoPulso(LocalDateTime u)    { this.ultimoPulso = u; }

    public LocalDateTime getTempoParadaInicio()    { return tempoParadaInicio; }
    public void setTempoParadaInicio(LocalDateTime t) { this.tempoParadaInicio = t; }

    public LocalDateTime getTempoFuncionamentoInicio() { return tempoFuncionamentoInicio; }
    public void setTempoFuncionamentoInicio(LocalDateTime t) { this.tempoFuncionamentoInicio = t; }

    public LocalDateTime getExcluidoEm()           { return excluidoEm; }
    public void setExcluidoEm(LocalDateTime excluidoEm) { this.excluidoEm = excluidoEm; }

    public String getExcluidoPor()                 { return excluidoPor; }
    public void setExcluidoPor(String excluidoPor) { this.excluidoPor = excluidoPor; }

    public List<Sensor> getSensores()              { return sensores; }
    public void setSensores(List<Sensor> sensores) { this.sensores = sensores; }

    public List<Alerta> getAlertas()               { return alertas; }
    public void setAlertas(List<Alerta> alertas)   { this.alertas = alertas; }

    public List<EventoEsteira> getEventos()        { return eventos; }
    public void setEventos(List<EventoEsteira> ev) { this.eventos = ev; }

    public boolean isExcluido() {
        return excluidoEm != null;
    }

    // Formatadores úteis para UI
    @JsonProperty("tempoParadoFormatado")
    public String getTempoParadoFormatado() {
        if (estado != EstadoEsteira.STOPPED || tempoParadaInicio == null) return "00:00:00";
        long s = Duration.between(tempoParadaInicio, LocalDateTime.now()).getSeconds();
        if (s < 0) s = 0;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    @JsonProperty("tempoFuncionandoFormatado")
    public String getTempoFuncionandoFormatado() {
        if (estado != EstadoEsteira.RUNNING || tempoFuncionamentoInicio == null) return "00:00:00";
        long s = Duration.between(tempoFuncionamentoInicio, LocalDateTime.now()).getSeconds();
        if (s < 0) s = 0;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    @JsonProperty("ultimoPulsoTexto")
    public String getUltimoPulsoTexto() {
        if (ultimoPulso == null) return "Nunca detectado";
        return ultimoPulso.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @JsonProperty("ultimaComunicacaoTexto")
    public String getUltimaComunicacaoTexto() {
        if (ultimoContato == null) return "Nunca comunicou";
        return ultimoContato.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}