package com.cba.sdae.config;

import com.cba.sdae.model.*;
import com.cba.sdae.repository.*;
import com.cba.sdae.service.SensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"dev", "test"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EsteiraRepository     esteiraRepository;
    private final SensorService         sensorService;
    private final AlertaRepository      alertaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder       passwordEncoder;

    public DataSeeder(EsteiraRepository esteiraRepository,
                      SensorService sensorService,
                      AlertaRepository alertaRepository,
                      FuncionarioRepository funcionarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.esteiraRepository     = esteiraRepository;
        this.sensorService         = sensorService;
        this.alertaRepository      = alertaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // ── 1. Funcionários: garante que o sistema sempre tenha administradores válidos (apenas em dev/test) ──
        if (funcionarioRepository.count() == 0) {
            log.info("Nenhum funcionário cadastrado — inserindo usuários padrão com senhas em BCrypt...");
            funcionarioRepository.save(criarFuncionario("Administrador",    "admin@sdae.com.br",  "admin123", "ADMIN001", "ADMINISTRADOR", "TI",         "MANHA"));
            funcionarioRepository.save(criarFuncionario("Carlos Engenheiro","carlos@sdae.com.br", "eng123",   "ENG001",   "ENGENHEIRO",    "Manutenção", "TARDE"));
            funcionarioRepository.save(criarFuncionario("Gustavo Operador", "gustavo@sdae.com.br","op123",    "OP001",    "OPERADOR",      "Operações",  "NOITE"));
            log.info("Usuários padrão criados. Credenciais iniciais não são registradas em log.");
        } else {
            // Se já existem usuários no banco, converte qualquer senha que esteja em texto puro para BCrypt
            funcionarioRepository.findAll().forEach(func -> {
                if (func.getSenha() != null && !func.getSenha().startsWith("$2")) {
                    func.setSenha(passwordEncoder.encode(func.getSenha()));
                    funcionarioRepository.save(func);
                    log.info("Senha em texto puro do usuário '{}' foi convertida automaticamente para BCrypt.", func.getEmail());
                }
            });
        }

        // ── 2. Esteiras e Sensores de Exemplo (apenas se o banco estiver vazio) ──
        if (esteiraRepository.count() == 0) {
            log.info("Inserindo esteiras e sensores iniciais...");

            Esteira e1 = esteiraRepository.save(new Esteira("Esteira 01 — Britagem Primária",    "Setor A — Mina Norte",       true));
            Esteira e2 = esteiraRepository.save(new Esteira("Esteira 02 — Transporte Principal", "Setor B — Corredor Central", true));
            Esteira e3 = esteiraRepository.save(new Esteira("Esteira 03 — Alimentação do Silo",  "Setor C — Beneficiamento",   false));

            // Sensores (NivelRisco calculado automaticamente pelo SensorService)
            sensorService.salvar(new Sensor("Sensor Laser LDR 01", 0.0, NivelRisco.NORMAL, e1));
            sensorService.salvar(new Sensor("Sensor Laser LDR 02", 0.0, NivelRisco.NORMAL, e2));

            // Alertas históricos de exemplo
            alertaRepository.save(new Alerta("Sistema iniciado e calibrado.",
                    LocalDateTime.now(), NivelRisco.NORMAL, e1));

            log.info("Dados de esteiras e sensores iniciais carregados.");
        }
    }

    // Monta um Funcionario com todos os campos
    private Funcionario criarFuncionario(String nome, String email, String senhaPura,
                                         String matricula, String nivel,
                                         String depto, String turno) {
        Funcionario f = new Funcionario(nome, email, nivel, turno);
        f.setMatricula(matricula);
        f.setNivelAcesso(nivel);
        f.setDepartamento(depto);
        // CORRIGIDO: nunca gravar senhaPura diretamente — sempre passar pelo encoder.
        f.setSenha(passwordEncoder.encode(senhaPura));
        return f;
    }
}