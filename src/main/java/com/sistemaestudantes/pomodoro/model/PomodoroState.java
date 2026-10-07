package com.sistemaestudantes.pomodoro.model;

public enum PomodoroState {
    FOCUS(
        25 * 60,
        "Foco nos Estudos",
        "Concentre-se na sua tarefa atual sem distrações."
    ),
    SHORT_BREAK(
        5 * 60,
        "Descanso Merecido",
        "Pare tudo! Respire, alongue-se e beba água."
    );

    private final int defaultDurationSeconds;
    private final String title;
    private final String description;

    PomodoroState(int defaultDurationSeconds, String title, String description) {
        this.defaultDurationSeconds = defaultDurationSeconds;
        this.title = title;
        this.description = description;
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

    /**
     * Alterna para o próximo estado de acordo com o ciclo Pomodoro padrão:
     * FOCUS -> SHORT_BREAK -> FOCUS.
     */
    public PomodoroState getNextState() {
        return this == FOCUS ? SHORT_BREAK : FOCUS;
    }
}
