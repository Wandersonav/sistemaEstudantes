package com.sistemaestudantes.scheduling.domain;

public enum SyncStatus {
    NAO_SINCRONIZADO("Não Sincronizado"),
    PENDENTE("Sincronizando..."),
    SINCRONIZADO("Sincronizado (Google Calendar)"),
    FALHA("Falha na Sincronização");

    private final String label;

    SyncStatus(String label) {
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
