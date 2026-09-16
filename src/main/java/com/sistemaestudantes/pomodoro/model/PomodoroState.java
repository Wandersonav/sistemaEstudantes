package com.sistemaestudantes.pomodoro.model;

import java.awt.Color;

/**
 * Representa os estados do ciclo Pomodoro.
 * 
 * - FOCUS: período de 25 minutos dedicados a estudo ou tarefa.
 * - SHORT_BREAK: período de 5 minutos dedicados ao descanso e relaxamento.
 */
public enum PomodoroState {
    FOCUS(
        25 * 60,
        "Foco nos Estudos",
        "Concentre-se na sua tarefa atual sem distrações.",
        new Color(235, 87, 87),    // Coral vibrante
        new Color(180, 50, 50)     // Cor escura de contraste
    ),
    SHORT_BREAK(
        5 * 60,
        "Descanso Merecido",
        "Pare tudo! Respire, alongue-se e beba água.",
        new Color(46, 204, 113),   // Verde esmeralda suave
        new Color(30, 140, 80)     // Cor escura de contraste
    );

    private final int defaultDurationSeconds;
    private final String title;
    private final String description;
    private final Color primaryColor;
    private final Color darkAccentColor;

    PomodoroState(int defaultDurationSeconds, String title, String description, Color primaryColor, Color darkAccentColor) {
        this.defaultDurationSeconds = defaultDurationSeconds;
        this.title = title;
        this.description = description;
        this.primaryColor = primaryColor;
        this.darkAccentColor = darkAccentColor;
    }

    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getDarkAccentColor() {
        return darkAccentColor;
    }

    /**
     * Alterna para o próximo estado de acordo com o ciclo Pomodoro padrão:
     * FOCUS -> SHORT_BREAK -> FOCUS.
     */
    public PomodoroState getNextState() {
        return this == FOCUS ? SHORT_BREAK : FOCUS;
    }
}
