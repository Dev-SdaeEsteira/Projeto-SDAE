package com.cba.sdae.repository;

import com.cba.sdae.model.Esteira;
import com.cba.sdae.model.EstadoEsteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsteiraRepository extends JpaRepository<Esteira, Long> {
    long countByAtiva(Boolean ativa);

    long countByEstado(EstadoEsteira estado);

    long countByAtivaAndEstado(Boolean ativa, EstadoEsteira estado);

    List<Esteira> findByEstado(EstadoEsteira estado);
}