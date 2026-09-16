package com.sistemaestudantes.scheduling.domain;

import java.awt.Color;

/**
 * Status da sincronização externa com o Google Calendar via MCP.
 */
public enum SyncStatus {
    NAO_SINCRONIZADO("Não Sincronizado", new Color(140, 150, 165)),
    PENDENTE("Sincronizando...", new Color(243, 156, 18)),
    SINCRONIZADO("Sincronizado (Google Calendar)", new Color(39, 174, 96)),
    FALHA("Falha na Sincronização", new Color(235, 87, 87));

    private final String label;
    private final Color color;

    SyncStatus(String label, Color color) {
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
