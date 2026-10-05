package com.cba.sdae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persiste cada leitura enviada por um sensor.
 * Endpoint: POST /api/sensores/{id}/leitura
 */
@Entity
@Table(name = "leituras_historico")
public class LeituraHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    @JsonIgnore
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esteira_id")
    @JsonIgnore
    private Esteira esteira;

    // Valor LDR bruto (0 a 4095)
    @Column(name = "valor_ldr")
    private Integer valorLdr;

    // Laser detectado (true se feixe presente, false se interrompido)
    @Column(name = "laser_detectado")
    private Boolean laserDetectado = false;

    // Pulso detectado neste ciclo
    @Column(name = "pulso_detectado")
    private Boolean pulsoDetectado = false;

    // Estado da esteira no momento da leitura
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_esteira")
    private EstadoEsteira estadoEsteira = EstadoEsteira.STOPPED;

    // DataHora da leitura
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Campos legados mantidos para compatibilidade total
    @Column(nullable = false)
    private Double valor = 0.0;          // leitura em %

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco nivelRisco = NivelRisco.NORMAL;

    // ── Construtores ──────────────────────────────────────────────
    public LeituraHistorico() {}

    public LeituraHistorico(Double valor, NivelRisco nivelRisco,
                            LocalDateTime dataHora, Sensor sensor) {
        this.valor      = valor;
        this.nivelRisco = nivelRisco;
        this.dataHora   = dataHora;
        this.sensor     = sensor;
        if (sensor != null) {
            this.esteira = sensor.getEsteira();
            if (sensor.getEsteira() != null) {
                this.estadoEsteira = sensor.getEsteira().getEstado();
            }
            this.valorLdr = sensor.getUltimoValorLdr();
            this.laserDetectado = sensor.getLaserDetectado();
        }
    }

    public LeituraHistorico(Sensor sensor, Esteira esteira, Integer valorLdr, Boolean laserDetectado,
                            Boolean pulsoDetectado, EstadoEsteira estadoEsteira, Double valor,
                            NivelRisco nivelRisco, LocalDateTime dataHora) {
        this.sensor = sensor;
        this.esteira = esteira;
        this.valorLdr = valorLdr;
        this.laserDetectado = laserDetectado;
        this.pulsoDetectado = pulsoDetectado;
        this.estadoEsteira = estadoEsteira;
        this.valor = valor != null ? valor : 0.0;
        this.nivelRisco = nivelRisco != null ? nivelRisco : NivelRisco.NORMAL;
        this.dataHora = dataHora != null ? dataHora : LocalDateTime.now();
    }

    // ── Getters e Setters ─────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public Sensor getSensor()                  { return sensor; }
    public void setSensor(Sensor sensor)       { this.sensor = sensor; }

    public Esteira getEsteira()                { return esteira; }
    public void setEsteira(Esteira esteira)    { this.esteira = esteira; }

    public Integer getValorLdr()               { return valorLdr; }
    public void setValorLdr(Integer valorLdr)  { this.valorLdr = valorLdr; }

    public Boolean getLaserDetectado()         { return laserDetectado; }
    public void setLaserDetectado(Boolean l)   { this.laserDetectado = l; }

    public Boolean getPulsoDetectado()         { return pulsoDetectado; }
    public void setPulsoDetectado(Boolean p)   { this.pulsoDetectado = p; }

    public EstadoEsteira getEstadoEsteira()    { return estadoEsteira; }
    public void setEstadoEsteira(EstadoEsteira e) { this.estadoEsteira = e; }

    public LocalDateTime getDataHora()         { return dataHora; }
    public void setDataHora(LocalDateTime d)   { this.dataHora = d; }

    public Double getValor()                   { return valor; }
    public void setValor(Double valor)         { this.valor = valor; }

    public NivelRisco getNivelRisco()          { return nivelRisco; }
    public void setNivelRisco(NivelRisco n)    { this.nivelRisco = n; }

    @JsonProperty("sensorId")
    public Long getSensorId() {
        return sensor != null ? sensor.getId() : null;
    }

    @JsonProperty("sensorModelo")
    public String getSensorModelo() {
        return sensor != null ? sensor.getModelo() : null;
    }

    @JsonProperty("esteiraId")
    public Long getEsteiraId() {
        return esteira != null ? esteira.getId() : (sensor != null && sensor.getEsteira() != null ? sensor.getEsteira().getId() : null);
    }

    @JsonProperty("esteiraNome")
    public String getEsteiraNome() {
        return esteira != null ? esteira.getNome() : (sensor != null && sensor.getEsteira() != null ? sensor.getEsteira().getNome() : null);
    }

    @JsonProperty("dataHoraFormatada")
    public String getDataHoraFormatada() {
        if (dataHora == null) return "—";
        return dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}