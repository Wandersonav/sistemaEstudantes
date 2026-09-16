package com.sistemaestudantes.pomodoro.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de domínio para o ciclo Pomodoro.
 * Responsável pelo gerenciamento do tempo, estados e contagem de ciclos.
 */
public class PomodoroModel {

    private PomodoroState currentState;
    private int remainingSeconds;
    private int totalSeconds;
    private int completedFocusCycles;
    private boolean isRunning;

    private final List<PomodoroListener> listeners = new ArrayList<>();

    public PomodoroModel() {
        this.currentState = PomodoroState.FOCUS;
        this.totalSeconds = currentState.getDefaultDurationSeconds();
        this.remainingSeconds = totalSeconds;
        this.completedFocusCycles = 0;
        this.isRunning = false;
    }

    public void addListener(PomodoroListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PomodoroListener listener) {
        listeners.remove(listener);
    }

    /**
     * Decrementa um segundo do temporizador.
     * Deve ser invocado periodicamente pelo controlador.
     */
    public void tick() {
        if (!isRunning) {
            return;
        }

        if (remainingSeconds > 0) {
            remainingSeconds--;
            notifyTick();
        }

        if (remainingSeconds == 0) {
            // Conclusão do período atual
            isRunning = false;
            notifyRunningStatusChanged(false);

            PomodoroState finishedState = currentState;
            if (finishedState == PomodoroState.FOCUS) {
                completedFocusCycles++;
            }

            notifyTimerFinished(finishedState);

            // Alterna para o próximo estado do Pomodoro
            currentState = currentState.getNextState();
            totalSeconds = currentState.getDefaultDurationSeconds();
            remainingSeconds = totalSeconds;

            notifyStateChanged();
            notifyTick();
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            notifyRunningStatusChanged(true);
        }
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            notifyRunningStatusChanged(false);
        }
    }

    public void togglePlayPause() {
        if (isRunning) {
            pause();
        } else {
            start();
        }
    }

    /**
     * Reinicia o temporizador para o tempo total do estado atual.
     */
    public void reset() {
        pause();
        remainingSeconds = totalSeconds;
        notifyTick();
    }

    /**
     * Pula a etapa atual e avança para o próximo estado (Foco <-> Descanso).
     */
    public void skip() {
        pause();
        currentState = currentState.getNextState();
        totalSeconds = currentState.getDefaultDurationSeconds();
        remainingSeconds = totalSeconds;
        notifyStateChanged();
        notifyTick();
    }

    /**
     * Retorna o tempo restante formatado como "MM:SS".
     */
    public String getFormattedTime() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Retorna a fração de progresso decorrido (0.0 até 1.0).
     */
    public double getProgressFraction() {
        if (totalSeconds == 0) return 0.0;
        return 1.0 - ((double) remainingSeconds / totalSeconds);
    }

    // Getters
    public PomodoroState getCurrentState() {
        return currentState;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public int getCompletedFocusCycles() {
        return completedFocusCycles;
    }

    public boolean isRunning() {
        return isRunning;
    }

    // Notificações para ouvintes
    private void notifyTick() {
        double progress = getProgressFraction();
        for (PomodoroListener listener : listeners) {
            listener.onTick(remainingSeconds, totalSeconds, progress);
        }
    }

    private void notifyStateChanged() {
        for (PomodoroListener listener : listeners) {
            listener.onStateChanged(currentState, completedFocusCycles);
        }
    }

    private void notifyTimerFinished(PomodoroState finishedState) {
        for (PomodoroListener listener : listeners) {
            listener.onTimerFinished(finishedState);
        }
    }

    private void notifyRunningStatusChanged(boolean running) {
        for (PomodoroListener listener : listeners) {
            listener.onRunningStatusChanged(running);
        }
    }
}
