package com.cba.sdae.model;

public enum TipoEventoEsteira {
    START("Início de Operação"),
    STOP("Parada de Esteira"),
    OFFLINE("Comunicação Perdida (Offline)"),
    ONLINE("Comunicação Restabelecida"),
    LASER_LOST("Feixe de Laser Interrompido/Perdido"),
    LASER_DETECTED("Feixe de Laser Detectado"),
    MAINTENANCE_START("Início de Manutenção"),
    MAINTENANCE_END("Fim de Manutenção"),
    ERROR("Falha / Erro");

    private final String descricao;

    TipoEventoEsteira(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
