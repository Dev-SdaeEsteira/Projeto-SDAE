package com.cba.sdae.service;

import com.cba.sdae.model.Relatorio;
import com.cba.sdae.repository.RelatorioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RelatorioService {

    private final RelatorioRepository relatorioRepository;

    public RelatorioService(RelatorioRepository relatorioRepository) {
        this.relatorioRepository = relatorioRepository;
    }

    public List<Relatorio> listarTodos() {
        return relatorioRepository.findAll();
    }

    public Optional<Relatorio> buscarPorId(Long id) {
        return relatorioRepository.findById(id);
    }

    public Relatorio salvar(Relatorio relatorio) {
        return relatorioRepository.save(relatorio);
    }

    public void deletar(Long id) {
        relatorioRepository.deleteById(id);
    }
}