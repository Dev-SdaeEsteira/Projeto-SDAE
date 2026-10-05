package com.cba.sdae.model;

public enum EstadoEsteira {
    RUNNING("Funcionando", "green"),
    STOPPED("Parada", "red"),
    OFFLINE("Offline", "gray"),
    MAINTENANCE("Manutenção", "blue"),
    ERROR("Erro", "orange");

    private final String descricao;
    private final String cor;

    EstadoEsteira(String descricao, String cor) {
        this.descricao = descricao;
        this.cor = cor;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCor() {
        return cor;
    }
}
