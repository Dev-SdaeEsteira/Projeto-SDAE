package com.cba.sdae.repository;

import com.cba.sdae.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByEmail(String email);

    Optional<Funcionario> findByMatricula(String matricula);

    boolean existsByEmail(String email);

    boolean existsByMatricula(String matricula);

    List<Funcionario> findByCargo(String cargo);

    long countByCargo(String cargo);

    List<Funcionario> findByNivelAcesso(String nivelAcesso);

    long countByNivelAcesso(String nivelAcesso);
}