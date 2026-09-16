package com.sistemaestudantes.scheduling.domain;

import java.awt.Color;

/**
 * Representa os tipos de atividades de estudo possíveis em uma sessão.
 */
public enum ActivityType {
    TEORIA("Teoria e Leitura", "Estudo de conceitos, videoaulas e livros", new Color(66, 133, 244)),
    EXERCICIOS("Resolução de Exercícios", "Prática ativa, listas de problemas e fixação", new Color(244, 180, 0)),
    REVISAO("Revisão Espaçada", "Flashcards, resumos e revisão periódica", new Color(15, 157, 88)),
    SIMULADO("Simulado e Provas", "Treino de tempo real e avaliação de desempenho", new Color(171, 71, 188));

    private final String label;
    private final String description;
    private final Color badgeColor;

    ActivityType(String label, String description, Color badgeColor) {
        this.label = label;
        this.description = description;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    @Override
    public String toString() {
        return label;
    }
}
