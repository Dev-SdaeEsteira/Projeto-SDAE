package com.cba.sdae.service;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.repository.AlertaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    // Retorna todos os alertas — do mais recente ao mais antigo
    public List<Alerta> listarTodos() {
        return alertaRepository.findAllByOrderByDataHoraDesc();
    }

    public List<Alerta> listarMaisRecentes(int limite) {
        int qtd = Math.clamp(limite, 1, 50);
        return alertaRepository.findByOrderByDataHoraDesc(PageRequest.of(0, qtd));
    }

    // Busca um alerta pelo ID
    public Optional<Alerta> buscarPorId(Long id) {
        return alertaRepository.findById(id);
    }

    // Busca todos os alertas de uma esteira
    public List<Alerta> listarPorEsteira(Long esteiraId) {
        return alertaRepository.findByEsteira_Id(esteiraId);
    }

    // Busca alertas por nível de risco
    public List<Alerta> listarPorNivelRisco(NivelRisco nivelRisco) {
        return alertaRepository.findByNivelRisco(nivelRisco);
    }

    public long contarTotal() {
        return alertaRepository.count();
    }

    public long contarPorNivelRisco(NivelRisco nivelRisco) {
        return alertaRepository.countByNivelRisco(nivelRisco);
    }

    // Salva ou atualiza um alerta
    public Alerta salvar(Alerta alerta) {
        return alertaRepository.save(alerta);
    }

    // Deleta um alerta pelo ID
    public void deletar(Long id) {
        alertaRepository.deleteById(id);
    }
}