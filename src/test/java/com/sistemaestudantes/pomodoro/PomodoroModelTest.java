package com.sistemaestudantes.pomodoro;

import com.sistemaestudantes.pomodoro.model.PomodoroListener;
import com.sistemaestudantes.pomodoro.model.PomodoroModel;
import com.sistemaestudantes.pomodoro.model.PomodoroState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o modelo do Pomodoro.
 */
class PomodoroModelTest {

    private PomodoroModel model;

    @BeforeEach
    void setUp() {
        model = new PomodoroModel();
    }

    @Test
    @DisplayName("Deve inicializar no estado de FOCO com duração de 25 minutos (1500 segundos)")
    void testInitialStateIsFocusWith25Minutes() {
        assertEquals(PomodoroState.FOCUS, model.getCurrentState());
        assertEquals(25 * 60, model.getRemainingSeconds());
        assertEquals(25 * 60, model.getTotalSeconds());
        assertEquals("25:00", model.getFormattedTime());
        assertEquals(0, model.getCompletedFocusCycles());
        assertFalse(model.isRunning());
    }

    @Test
    @DisplayName("Deve iniciar e pausar o temporizador corretamente")
    void testStartAndPause() {
        model.start();
        assertTrue(model.isRunning());

        model.pause();
        assertFalse(model.isRunning());
    }

    @Test
    @DisplayName("Deve decrementar o tempo ao executar o tick enquanto estiver ativo")
    void testTickDecrementsRemainingSeconds() {
        model.start();
        model.tick();

        assertEquals(1500 - 1, model.getRemainingSeconds());
        assertEquals("24:59", model.getFormattedTime());
        assertTrue(model.getProgressFraction() > 0.0);
    }

    @Test
    @DisplayName("Não deve decrementar tempo se o temporizador estiver pausado")
    void testTickDoesNothingWhenPaused() {
        model.tick();
        assertEquals(1500, model.getRemainingSeconds());
    }

    @Test
    @DisplayName("Deve transitar de FOCO (25 min) para DESCANSO (5 min) ao atingir zero e incrementar o ciclo")
    void testTransitionFromFocusToBreak() {
        AtomicBoolean finishedCalled = new AtomicBoolean(false);
        AtomicReference<PomodoroState> finishedStateRef = new AtomicReference<>();

        model.addListener(new PomodoroListener() {
            @Override
            public void onTick(int remainingSeconds, int totalSeconds, double progress) {}

            @Override
            public void onStateChanged(PomodoroState newState, int completedCycles) {}

            @Override
            public void onTimerFinished(PomodoroState finishedState) {
                finishedCalled.set(true);
                finishedStateRef.set(finishedState);
            }

            @Override
            public void onRunningStatusChanged(boolean isRunning) {}
        });

        model.start();

        // Simula o avanço de todos os 1500 segundos
        for (int i = 0; i < 1500; i++) {
            model.tick();
        }

        assertTrue(finishedCalled.get(), "O ouvinte onTimerFinished deve ser chamado");
        assertEquals(PomodoroState.FOCUS, finishedStateRef.get());
        assertEquals(PomodoroState.SHORT_BREAK, model.getCurrentState());
        assertEquals(5 * 60, model.getRemainingSeconds());
        assertEquals(5 * 60, model.getTotalSeconds());
        assertEquals("05:00", model.getFormattedTime());
        assertEquals(1, model.getCompletedFocusCycles());
        assertFalse(model.isRunning(), "O temporizador deve pausar ao trocar de etapa para aguardar ação do usuário");
    }

    @Test
    @DisplayName("Deve alternar entre FOCO e DESCANSO através do método skip()")
    void testSkipTogglesState() {
        assertEquals(PomodoroState.FOCUS, model.getCurrentState());

        model.skip();
        assertEquals(PomodoroState.SHORT_BREAK, model.getCurrentState());
        assertEquals(5 * 60, model.getRemainingSeconds());
        assertEquals("05:00", model.getFormattedTime());

        model.skip();
        assertEquals(PomodoroState.FOCUS, model.getCurrentState());
        assertEquals(25 * 60, model.getRemainingSeconds());
        assertEquals("25:00", model.getFormattedTime());
    }

    @Test
    @DisplayName("Deve reiniciar o tempo do estado corrente com reset()")
    void testReset() {
        model.start();
        model.tick();
        model.tick();
        model.tick();

        assertEquals(1497, model.getRemainingSeconds());

        model.reset();
        assertEquals(1500, model.getRemainingSeconds());
        assertEquals("25:00", model.getFormattedTime());
        assertFalse(model.isRunning());
    }
}
