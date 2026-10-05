package com.cba.sdae.service;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.EstadoEsteira;
import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.model.TipoEventoEsteira;
import com.cba.sdae.repository.AlertaRepository;
import com.cba.sdae.repository.EsteiraRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EsteiraService {

    private final EsteiraRepository    esteiraRepository;
    private final AuditoriaService     auditoriaService;
    private final EventoEsteiraService eventoService;
    private final AlertaRepository     alertaRepository;

    public EsteiraService(EsteiraRepository esteiraRepository,
                          AuditoriaService auditoriaService,
                          EventoEsteiraService eventoService,
                          AlertaRepository alertaRepository) {
        this.esteiraRepository = esteiraRepository;
        this.auditoriaService  = auditoriaService;
        this.eventoService     = eventoService;
        this.alertaRepository  = alertaRepository;
    }

    // Retorna todas as esteiras
    public List<Esteira> listarTodas() {
        return esteiraRepository.findAll();
    }

    // Busca uma esteira pelo ID — retorna vazio se não encontrar
    public Optional<Esteira> buscarPorId(Long id) {
        return esteiraRepository.findById(id);
    }

    public long contarTotal()       { return esteiraRepository.count(); }
    public long contarAtivas()      { return esteiraRepository.countByAtiva(true); }
    public long contarInativas()    { return esteiraRepository.countByAtiva(false); }

    // Contadores de estado real
    public long contarFuncionando() { return esteiraRepository.countByEstado(EstadoEsteira.RUNNING); }
    public long contarParadas()     { return esteiraRepository.countByEstado(EstadoEsteira.STOPPED); }
    public long contarOffline()     { return esteiraRepository.countByEstado(EstadoEsteira.OFFLINE); }
    public long contarManutencao()  { return esteiraRepository.countByEstado(EstadoEsteira.MAINTENANCE); }
    public long contarErro()        { return esteiraRepository.countByEstado(EstadoEsteira.ERROR); }

    // Salva uma esteira nova ou atualiza uma existente
    @Transactional
    public Esteira salvar(Esteira esteira) {
        if (esteira == null
                || esteira.getNome() == null || esteira.getNome().isBlank()
                || esteira.getLocalizacao() == null || esteira.getLocalizacao().isBlank()) {
            throw new IllegalArgumentException("Nome e localização da esteira são obrigatórios.");
        }
        if (esteira.getAtiva() == null) {
            esteira.setAtiva(true);
        }
        if (esteira.getEstado() == null) {
            esteira.setEstado(EstadoEsteira.STOPPED);
        }
        
        boolean isNova = esteira.getId() == null;
        Esteira salva = esteiraRepository.save(esteira);
        
        // Registrar auditoria
        String usuario = getUsuarioAtual();
        String acao = isNova ? "CRIAR" : "ATUALIZAR";
        String detalhes = String.format("Nome: %s, Localização: %s, Ativa: %s, Estado: %s", 
            salva.getNome(), salva.getLocalizacao(), salva.getAtiva(), salva.getEstado());
        auditoriaService.registrar(acao, "ESTEIRA", salva.getId(), usuario, detalhes);
        
        return salva;
    }

    @Transactional
    public Esteira alternarManutencao(Long id, boolean entrar, String motivo) {
        return buscarPorId(id).map(esteira -> {
            EstadoEsteira anterior = esteira.getEstado();
            String usuario = getUsuarioAtual();
            LocalDateTime agora = LocalDateTime.now();

            if (entrar) {
                esteira.setEstado(EstadoEsteira.MAINTENANCE);
                eventoService.registrarTransicao(esteira, TipoEventoEsteira.MAINTENANCE_START,
                        anterior, EstadoEsteira.MAINTENANCE, "Manutenção iniciada por " + usuario + ": " + (motivo != null ? motivo : "Operação programada"));
                alertaRepository.save(new Alerta("Esteira '" + esteira.getNome() + "' colocada em MANUTENÇÃO por " + usuario + ".",
                        agora, NivelRisco.ATENCAO, esteira));
                auditoriaService.registrar("MANUTENCAO_INICIO", "ESTEIRA", id, usuario, "Motivo: " + motivo);
            } else {
                esteira.setEstado(EstadoEsteira.STOPPED);
                esteira.setTempoParadaInicio(agora);
                eventoService.registrarTransicao(esteira, TipoEventoEsteira.MAINTENANCE_END,
                        anterior, EstadoEsteira.STOPPED, "Manutenção encerrada por " + usuario);
                alertaRepository.save(new Alerta("Manutenção da esteira '" + esteira.getNome() + "' encerrada.",
                        agora, NivelRisco.NORMAL, esteira));
                auditoriaService.registrar("MANUTENCAO_FIM", "ESTEIRA", id, usuario, "Finalizada com sucesso");
            }
            return esteiraRepository.save(esteira);
        }).orElseThrow(() -> new IllegalArgumentException("Esteira não encontrada: " + id));
    }

    // Soft-delete uma esteira pelo ID
    @Transactional
    public void deletar(Long id) {
        esteiraRepository.findById(id).ifPresent(esteira -> {
            String usuario = getUsuarioAtual();
            esteira.setExcluidoEm(LocalDateTime.now());
            esteira.setExcluidoPor(usuario);
            esteiraRepository.save(esteira);
            
            // Registrar auditoria
            String detalhes = String.format("Nome: %s, Localização: %s", 
                esteira.getNome(), esteira.getLocalizacao());
            auditoriaService.registrar("EXCLUIR", "ESTEIRA", id, usuario, detalhes);
        });
    }

    private String getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "sistema";
    }
}