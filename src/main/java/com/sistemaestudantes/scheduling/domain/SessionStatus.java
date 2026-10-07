package com.sistemaestudantes.scheduling.domain;

public enum SessionStatus {
    PLANEJADA("Planejada"),
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada");

    private final String label;

    SessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
