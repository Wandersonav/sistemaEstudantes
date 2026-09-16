package com.sistemaestudantes.pomodoro.controller;

import com.sistemaestudantes.pomodoro.audio.SoundNotifier;
import com.sistemaestudantes.pomodoro.model.PomodoroListener;
import com.sistemaestudantes.pomodoro.model.PomodoroModel;
import com.sistemaestudantes.pomodoro.model.PomodoroState;

import javax.swing.Timer;

/**
 * Controlador responsável por gerenciar a contagem do tempo no Event Dispatch Thread (EDT)
 * e coordenar ações entre a interface e o modelo.
 */
public class PomodoroController implements PomodoroListener {

    private final PomodoroModel model;
    private final Timer swingTimer;

    public PomodoroController(PomodoroModel model) {
        if (model == null) {
            throw new IllegalArgumentException("O modelo Pomodoro não pode ser nulo.");
        }
        this.model = model;
        this.model.addListener(this);

        // Timer disparado a cada 1000 milissegundos (1 segundo) diretamente no EDT do Swing
        this.swingTimer = new Timer(1000, e -> this.model.tick());
    }

    public void start() {
        model.start();
        if (!swingTimer.isRunning()) {
            swingTimer.start();
        }
    }

    public void pause() {
        model.pause();
        if (swingTimer.isRunning()) {
            swingTimer.stop();
        }
    }

    public void togglePlayPause() {
        if (model.isRunning()) {
            pause();
        } else {
            start();
        }
    }

    public void reset() {
        if (swingTimer.isRunning()) {
            swingTimer.stop();
        }
        model.reset();
    }

    public void skip() {
        if (swingTimer.isRunning()) {
            swingTimer.stop();
        }
        model.skip();
    }

    public PomodoroModel getModel() {
        return model;
    }

    @Override
    public void onTick(int remainingSeconds, int totalSeconds, double progress) {
        // Manipulado principalmente pelas visualizações
    }

    @Override
    public void onStateChanged(PomodoroState newState, int completedCycles) {
        // Se mudou de estado e o modelo pausou, sincroniza o timer
        if (!model.isRunning() && swingTimer.isRunning()) {
            swingTimer.stop();
        }
    }

    @Override
    public void onTimerFinished(PomodoroState finishedState) {
        // Emite alerta sonoro ao concluir ciclo de foco ou descanso
        SoundNotifier.playNotification();
    }

    @Override
    public void onRunningStatusChanged(boolean isRunning) {
        if (isRunning && !swingTimer.isRunning()) {
            swingTimer.start();
        } else if (!isRunning && swingTimer.isRunning()) {
            swingTimer.stop();
        }
    }
}
