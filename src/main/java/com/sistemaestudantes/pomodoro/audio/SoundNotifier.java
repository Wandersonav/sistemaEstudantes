package com.sistemaestudantes.pomodoro.audio;

import java.awt.Toolkit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Utilitário para emissão de alertas sonoros suaves ao término de ciclos.
 * Gera ondas senoidais sintetizadas via API padrão do Java (javax.sound.sampled),
 * com fallback seguro para Toolkit.beep().
 */
public final class SoundNotifier {

    private SoundNotifier() {
        // Construtor privado para utilitário estático
    }

    /**
     * Toca uma notificação sonora de conclusão em thread separada.
     */
    public static void playNotification() {
        new Thread(() -> {
            try {
                // Toca dois tons harmoniosos: 587 Hz (D5) seguido de 880 Hz (A5)
                playTone(587.33, 180);
                Thread.sleep(40);
                playTone(880.00, 320);
            } catch (Exception e) {
                // Fallback defensivo usando o beep padrão do sistema operacional
                try {
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {
                }
            }
        }, "PomodoroSoundThread").start();
    }

    private static void playTone(double freqHz, int durationMs) {
        final float sampleRate = 44100;
        final int numSamples = (int) (sampleRate * (durationMs / 1000.0));
        byte[] buffer = new byte[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double time = i / sampleRate;
            double angle = 2.0 * Math.PI * freqHz * time;

            // Envelope simples para suavizar o ataque e decaimento (evita cliques)
            double envelope = 1.0;
            double attack = 0.05 * numSamples;
            double decay = 0.25 * numSamples;
            if (i < attack) {
                envelope = i / attack;
            } else if (i > (numSamples - decay)) {
                envelope = (numSamples - i) / decay;
            }

            buffer[i] = (byte) (Math.sin(angle) * 127.0 * envelope * 0.4);
        }

        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
