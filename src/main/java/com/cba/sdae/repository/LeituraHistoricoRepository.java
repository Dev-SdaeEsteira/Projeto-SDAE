package com.cba.sdae.repository;

import com.cba.sdae.model.LeituraHistorico;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeituraHistoricoRepository extends JpaRepository<LeituraHistorico, Long> {

    /** Retorna as leituras mais recentes de um sensor paginadas diretamente no banco */
    List<LeituraHistorico> findBySensor_IdOrderByDataHoraDesc(Long sensorId, Pageable pageable);

    /** Retorna as 30 leituras mais recentes de um sensor */
    List<LeituraHistorico> findTop30BySensor_IdOrderByDataHoraDesc(Long sensorId);

    List<LeituraHistorico> findByEsteira_IdOrderByDataHoraDesc(Long esteiraId, Pageable pageable);

    List<LeituraHistorico> findAllByOrderByDataHoraDesc(Pageable pageable);

    List<LeituraHistorico> findBySensor_IdAndDataHoraAfterOrderByDataHoraAsc(Long sensorId, LocalDateTime dataHora);

    List<LeituraHistorico> findByEsteira_IdAndDataHoraAfterOrderByDataHoraAsc(Long esteiraId, LocalDateTime dataHora);

    @Query("SELECT l FROM LeituraHistorico l WHERE (:esteiraId IS NULL OR l.esteira.id = :esteiraId) AND (:sensorId IS NULL OR l.sensor.id = :sensorId) ORDER BY l.dataHora DESC")
    List<LeituraHistorico> filtrarLeituras(@Param("esteiraId") Long esteiraId, @Param("sensorId") Long sensorId, Pageable pageable);
}