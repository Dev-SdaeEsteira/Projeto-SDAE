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
import java.util.UUID;

@Entity
@Table(name = "sensores")
@Where(clause = "excluido_em IS NULL")
public class Sensor {

    // Tempo máximo sem receber leitura do ESP32 para ainda considerar o sensor "conectado"
    private static final long SEGUNDOS_LIMITE_CONEXAO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Modelo do sensor é obrigatório")
    @Size(min = 2, max = 50, message = "Modelo deve ter entre 2 e 50 caracteres")
    private String modelo;

    @Column(length = 255)
    private String descricao;

    @Column(length = 50)
    private String tipo = "Laser + LDR";

    @Column(length = 30)
    private String status = "ATIVO";

    // Leitura atual em porcentagem (0 a 100)
    @Column(nullable = false)
    @DecimalMin(value = "0.0", message = "Leitura deve ser maior ou igual a 0")
    @DecimalMax(value = "100.0", message = "Leitura deve ser menor ou igual a 100")
    private Double leituraAtual = 0.0;

    // Nível de risco calculado
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco nivelRisco = NivelRisco.NORMAL;

    // Telemetria do sensor LDR + Laser
    @Column(name = "ultimo_valor_ldr")
    private Integer ultimoValorLdr = 0;

    @Column(name = "laser_detectado")
    private Boolean laserDetectado = false;

    @Column(name = "ultimo_pulso")
    private LocalDateTime ultimoPulso;

    @Column(name = "quantidade_pulsos")
    private Long quantidadePulsos = 0L;

    @Column(name = "ultimo_contato")
    private LocalDateTime ultimoContato;

    // Muitos sensores pertencem a uma esteira
    @ManyToOne
    @JoinColumn(name = "esteira_id", nullable = false)
    @JsonIgnore
    private Esteira esteira;

    // Histórico de leituras vinculadas a este sensor
    @OneToMany(mappedBy = "sensor", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<LeituraHistorico> historico = new ArrayList<>();

    @Transient
    private Long esteiraIdTransient;

    // Chave usada pelo ESP32 para se autenticar ao enviar leituras
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "api_key", unique = true, length = 36)
    private String apiKey;

    // Data/hora da última leitura recebida do ESP32
    @Column(name = "ultima_leitura")
    private LocalDateTime ultimaLeitura;

    // Soft-delete: data de exclusão (null = ativo)
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    // Soft-delete: quem excluiu (null = não excluído)
    @Column(name = "excluido_por")
    private String excluidoPor;

    // Construtores
    public Sensor() {}

    public Sensor(String modelo, Double leituraAtual, NivelRisco nivelRisco, Esteira esteira) {
        this.modelo       = modelo;
        this.leituraAtual = leituraAtual;
        this.nivelRisco   = nivelRisco;
        this.esteira      = esteira;
    }

    // Getters e Setters
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getModelo()                  { return modelo; }
    public void setModelo(String modelo)       { this.modelo = modelo; }

    public String getDescricao()               { return descricao; }
    public void setDescricao(String d)         { this.descricao = d; }

    public String getTipo()                    { return tipo != null ? tipo : "Laser + LDR"; }
    public void setTipo(String tipo)           { this.tipo = tipo; }

    public String getStatus()                  { return status != null ? status : "ATIVO"; }
    public void setStatus(String status)       { this.status = status; }

    public Double getLeituraAtual()            { return leituraAtual; }
    public void setLeituraAtual(Double l)      { this.leituraAtual = l; }

    public NivelRisco getNivelRisco()          { return nivelRisco; }
    public void setNivelRisco(NivelRisco n)    { this.nivelRisco = n; }

    public Integer getUltimoValorLdr()         { return ultimoValorLdr; }
    public void setUltimoValorLdr(Integer v)   { this.ultimoValorLdr = v; }

    public Boolean getLaserDetectado()         { return laserDetectado; }
    public void setLaserDetectado(Boolean l)   { this.laserDetectado = l; }

    public LocalDateTime getUltimoPulso()      { return ultimoPulso; }
    public void setUltimoPulso(LocalDateTime u){ this.ultimoPulso = u; }

    public Long getQuantidadePulsos()          { return quantidadePulsos != null ? quantidadePulsos : 0L; }
    public void setQuantidadePulsos(Long q)    { this.quantidadePulsos = q != null ? q : 0L; }

    public LocalDateTime getUltimoContato()    { return ultimoContato; }
    public void setUltimoContato(LocalDateTime u) { this.ultimoContato = u; }

    public Esteira getEsteira()                { return esteira; }
    public void setEsteira(Esteira esteira)    { this.esteira = esteira; }

    public String getApiKey()                  { return apiKey; }
    public void setApiKey(String apiKey)       { this.apiKey = apiKey; }

    public LocalDateTime getUltimaLeitura()          { return ultimaLeitura; }
    public void setUltimaLeitura(LocalDateTime u)    { this.ultimaLeitura = u; }

    public LocalDateTime getExcluidoEm()           { return excluidoEm; }
    public void setExcluidoEm(LocalDateTime excluidoEm) { this.excluidoEm = excluidoEm; }

    public String getExcluidoPor()                 { return excluidoPor; }
    public void setExcluidoPor(String excluidoPor) { this.excluidoPor = excluidoPor; }

    public boolean isExcluido() {
        return excluidoEm != null;
    }

    // Gera a chave automaticamente na primeira vez que o sensor é salvo
    @PrePersist
    public void gerarApiKeySeNecessario() {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            this.apiKey = UUID.randomUUID().toString();
        }
    }

    @JsonProperty("conectado")
    public boolean isConectado() {
        LocalDateTime ref = ultimoContato != null ? ultimoContato : ultimaLeitura;
        if (ref == null) return false;
        long limite = (esteira != null && esteira.getOfflineTimeoutSeconds() != null)
                ? esteira.getOfflineTimeoutSeconds() : SEGUNDOS_LIMITE_CONEXAO;
        return Duration.between(ref, LocalDateTime.now()).getSeconds() <= limite;
    }

    @JsonProperty("ultimaLeituraTexto")
    public String getUltimaLeituraTexto() {
        LocalDateTime ref = ultimoContato != null ? ultimoContato : ultimaLeitura;
        if (ref == null) return "Nunca conectou";

        long segundos = Duration.between(ref, LocalDateTime.now()).getSeconds();
        if (segundos < 0) segundos = 0;

        if (segundos < 10)      return "Agora mesmo";
        if (segundos < 60)      return "Há " + segundos + "s";
        if (segundos < 3600)    return "Há " + (segundos / 60) + " min";
        if (segundos < 86400)   return "Há " + (segundos / 3600) + "h";
        return "Há " + (segundos / 86400) + " dia(s)";
    }

    @JsonProperty("esteiraId")
    public Long getEsteiraId() {
        return esteira != null ? esteira.getId() : null;
    }

    @JsonProperty("esteiraNome")
    public String getEsteiraNome() {
        return esteira != null ? esteira.getNome() : null;
    }

    @JsonProperty("esteiraLocalizacao")
    public String getEsteiraLocalizacao() {
        return esteira != null ? esteira.getLocalizacao() : null;
    }

    @JsonProperty("esteiraId")
    public void setEsteiraId(Long esteiraId) {
        this.esteiraIdTransient = esteiraId;
    }

    public Long getEsteiraIdTransient() {
        return esteiraIdTransient;
    }

    public List<LeituraHistorico> getHistorico() {
        return historico;
    }

    public void setHistorico(List<LeituraHistorico> historico) {
        this.historico = historico;
    }

    public String getNome() {
        return modelo;
    }

    public void setNome(String nome) {
        this.modelo = nome;
    }

    public Double getUltimaTrepidacao() {
        return leituraAtual;
    }

    @JsonProperty("ultimoPulsoTexto")
    public String getUltimoPulsoTexto() {
        if (ultimoPulso == null) return "—";
        return ultimoPulso.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}