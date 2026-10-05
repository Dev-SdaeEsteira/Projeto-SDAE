package com.cba.sdae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_esteira")
public class EventoEsteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esteira_id", nullable = false)
    @JsonIgnore
    private Esteira esteira;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoEsteira tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEsteira estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEsteira novoEstado;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column
    private LocalDateTime dataHoraFim;

    @Column
    private Long duracaoSegundos;

    @Column(length = 500)
    private String observacao;

    public EventoEsteira() {}

    public EventoEsteira(Esteira esteira, TipoEventoEsteira tipoEvento, EstadoEsteira estadoAnterior,
                         EstadoEsteira novoEstado, LocalDateTime dataHoraInicio, String observacao) {
        this.esteira = esteira;
        this.tipoEvento = tipoEvento;
        this.estadoAnterior = estadoAnterior;
        this.novoEstado = novoEstado;
        this.dataHoraInicio = dataHoraInicio;
        this.observacao = observacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Esteira getEsteira() {
        return esteira;
    }

    public void setEsteira(Esteira esteira) {
        this.esteira = esteira;
    }

    public TipoEventoEsteira getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoEsteira tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public EstadoEsteira getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoEsteira estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoEsteira getNovoEstado() {
        return novoEstado;
    }

    public void setNovoEstado(EstadoEsteira novoEstado) {
        this.novoEstado = novoEstado;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public Long getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public void setDuracaoSegundos(Long duracaoSegundos) {
        this.duracaoSegundos = duracaoSegundos;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @JsonProperty("esteiraId")
    public Long getEsteiraId() {
        return esteira != null ? esteira.getId() : null;
    }

    @JsonProperty("esteiraNome")
    public String getEsteiraNome() {
        return esteira != null ? esteira.getNome() : null;
    }

    @JsonProperty("duracaoFormatada")
    public String getDuracaoFormatada() {
        if (duracaoSegundos == null) {
            if (dataHoraInicio != null && dataHoraFim == null) {
                long segs = java.time.Duration.between(dataHoraInicio, LocalDateTime.now()).getSeconds();
                if (segs < 0) segs = 0;
                long m = segs / 60;
                long s = segs % 60;
                return String.format("%02d:%02d (em andamento)", m, s);
            }
            return "—";
        }
        long m = duracaoSegundos / 60;
        long s = duracaoSegundos % 60;
        return String.format("%02d:%02d", m, s);
    }
}
