package com.cba.sdae.service;

import com.cba.sdae.model.Auditoria;
import com.cba.sdae.repository.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String acao, String entidade, Long entidadeId, String usuario, String detalhes) {
        try {
            Auditoria auditoria = new Auditoria(acao, entidade, entidadeId, usuario, detalhes);
            auditoriaRepository.save(auditoria);
            log.debug("Auditoria registrada: {} {} por {}", acao, entidade, usuario);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria: {}", e.getMessage(), e);
            // Não lançar exceção para não interromper a operação principal
        }
    }
}
