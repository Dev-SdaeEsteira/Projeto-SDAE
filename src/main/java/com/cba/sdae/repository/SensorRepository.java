package com.cba.sdae.repository;

import com.cba.sdae.model.NivelRisco;
import com.cba.sdae.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findByEsteira_Id(Long esteiraId);

    List<Sensor> findByNivelRisco(NivelRisco nivelRisco);

    long countByNivelRisco(NivelRisco nivelRisco);

    @Query("SELECT DISTINCT s FROM Sensor s LEFT JOIN FETCH s.esteira")
    List<Sensor> findAllWithEsteira();

    @Query("SELECT COALESCE(MAX(s.leituraAtual), 0.0) FROM Sensor s")
    Double findMaxLeituraAtual();
}
