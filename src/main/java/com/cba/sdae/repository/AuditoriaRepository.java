package com.cba.sdae.repository;

import com.cba.sdae.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByEntidadeAndEntidadeId(String entidade, Long entidadeId);
    List<Auditoria> findByUsuario(String usuario);
    List<Auditoria> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
}
