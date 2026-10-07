package com.sistemaestudantes.scheduling.domain;

/**
 * Representa os tipos de atividades de estudo possíveis em uma sessão.
 */
public enum ActivityType {
    TEORIA("Teoria e Leitura", "Estudo de conceitos, videoaulas e livros", "#3A7D8C"),
    EXERCICIOS("Resolução de Exercícios", "Prática ativa, listas de problemas e fixação", "#D9A441"),
    REVISAO("Revisão Espaçada", "Flashcards, resumos e revisão periódica", "#4C9A7A"),
    SIMULADO("Simulado e Provas", "Treino de tempo real e avaliação de desempenho", "#7FB3D1");

    private final String label;
    private final String description;
    private final String hexColor;

    ActivityType(String label, String description, String hexColor) {
        this.label = label;
        this.description = description;
        this.hexColor = hexColor;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getHexColor() {
        return hexColor;
    }

    @Override
    public String toString() {
        return label;
    }
}
