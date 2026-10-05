package com.cba.sdae.repository;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.model.NivelRisco;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByEsteira_Id(Long esteiraId);

    List<Alerta> findByNivelRisco(NivelRisco nivelRisco);

    long countByNivelRisco(NivelRisco nivelRisco);

    @EntityGraph(attributePaths = "esteira")
    @Query("SELECT a FROM Alerta a ORDER BY a.dataHora DESC")
    List<Alerta> findAllByOrderByDataHoraDesc();

    @EntityGraph(attributePaths = "esteira")
    List<Alerta> findByOrderByDataHoraDesc(Pageable pageable);
}
