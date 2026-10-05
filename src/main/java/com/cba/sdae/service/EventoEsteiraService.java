package com.cba.sdae.service;

import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.EstadoEsteira;
import com.cba.sdae.model.EventoEsteira;
import com.cba.sdae.model.TipoEventoEsteira;
import com.cba.sdae.repository.EventoEsteiraRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoEsteiraService {

    private final EventoEsteiraRepository eventoRepository;

    public EventoEsteiraService(EventoEsteiraRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional
    public EventoEsteira registrarTransicao(Esteira esteira, TipoEventoEsteira tipoEvento,
                                             EstadoEsteira estadoAnterior, EstadoEsteira novoEstado,
                                             String observacao) {
        LocalDateTime agora = LocalDateTime.now();

        // Encerra evento anterior aberto se houver
        eventoRepository.findTopByEsteira_IdAndDataHoraFimIsNullOrderByDataHoraInicioDesc(esteira.getId())
                .ifPresent(aberto -> {
                    aberto.setDataHoraFim(agora);
                    long segs = Duration.between(aberto.getDataHoraInicio(), agora).getSeconds();
                    aberto.setDuracaoSegundos(Math.max(0, segs));
                    eventoRepository.save(aberto);
                });

        EventoEsteira novo = new EventoEsteira(esteira, tipoEvento, estadoAnterior, novoEstado, agora, observacao);
        return eventoRepository.save(novo);
    }

    public List<EventoEsteira> listarPorEsteira(Long esteiraId, int qtd) {
        return eventoRepository.findByEsteira_IdOrderByDataHoraInicioDesc(
                esteiraId, PageRequest.of(0, Math.clamp(qtd, 1, 100)));
    }

    public List<EventoEsteira> listarMaisRecentes(int qtd) {
        return eventoRepository.findAllByOrderByDataHoraInicioDesc(
                PageRequest.of(0, Math.clamp(qtd, 1, 100)));
    }
}
