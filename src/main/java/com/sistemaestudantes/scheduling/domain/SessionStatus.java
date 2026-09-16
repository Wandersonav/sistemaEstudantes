package com.sistemaestudantes.scheduling.domain;

import java.awt.Color;

/**
 * Estados do ciclo de vida de uma sessão de estudo.
 */
public enum SessionStatus {
    PLANEJADA("Planejada", new Color(130, 140, 155)),
    EM_ANDAMENTO("Em Andamento", new Color(52, 152, 219)),
    CONCLUIDA("Concluída", new Color(46, 204, 113)),
    CANCELADA("Cancelada", new Color(231, 76, 60));

    private final String label;
    private final Color color;

    SessionStatus(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return label;
    }
}
