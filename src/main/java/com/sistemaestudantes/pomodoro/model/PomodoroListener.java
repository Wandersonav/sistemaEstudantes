package com.sistemaestudantes.pomodoro.model;

/**
 * Interface ouvinte (Observer) para ser notificada sobre alterações no ciclo do Pomodoro.
 */
public interface PomodoroListener {

    /**
     * Invocado a cada segundo decorrido no temporizador.
     *
     * @param remainingSeconds Segundos restantes no ciclo atual.
     * @param totalSeconds Duração total prevista para o ciclo atual em segundos.
     * @param progress Percentual de progresso concluído entre 0.0 e 1.0.
     */
    void onTick(int remainingSeconds, int totalSeconds, double progress);

    /**
     * Invocado quando o estado do ciclo é alterado (ex: Foco -> Descanso).
     *
     * @param newState Novo estado ativo.
     * @param completedCycles Quantidade de ciclos completos de foco finalizados.
     */
    void onStateChanged(PomodoroState newState, int completedCycles);

    /**
     * Invocado no momento exato em que a contagem regressiva atinge zero.
     *
     * @param finishedState O estado que acabou de ser concluído.
     */
    void onTimerFinished(PomodoroState finishedState);

    /**
     * Invocado quando o temporizador é iniciado ou pausado.
     *
     * @param isRunning true se estiver executando, false se pausado ou parado.
     */
    void onRunningStatusChanged(boolean isRunning);
}
