package com.sistemaestudantes.scheduling.domain;

/**
 * Representa os tipos de atividades de estudo possíveis em uma sessão.
 */
public enum ActivityType {
    TEORIA("Teoria e Leitura", "Estudo de conceitos, videoaulas e livros", "#4285F4"),
    EXERCICIOS("Resolução de Exercícios", "Prática ativa, listas de problemas e fixação", "#F4B400"),
    REVISAO("Revisão Espaçada", "Flashcards, resumos e revisão periódica", "#0F9D58"),
    SIMULADO("Simulado e Provas", "Treino de tempo real e avaliação de desempenho", "#AB47BC");

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
