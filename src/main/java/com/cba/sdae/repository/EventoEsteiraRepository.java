package com.cba.sdae.repository;

import com.cba.sdae.model.EventoEsteira;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoEsteiraRepository extends JpaRepository<EventoEsteira, Long> {

    List<EventoEsteira> findByEsteira_IdOrderByDataHoraInicioDesc(Long esteiraId, Pageable pageable);

    Optional<EventoEsteira> findTopByEsteira_IdAndDataHoraFimIsNullOrderByDataHoraInicioDesc(Long esteiraId);

    List<EventoEsteira> findAllByOrderByDataHoraInicioDesc(Pageable pageable);

    long countByEsteira_Id(Long esteiraId);
}
