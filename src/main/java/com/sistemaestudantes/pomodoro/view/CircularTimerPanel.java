package com.sistemaestudantes.pomodoro.view;

import com.sistemaestudantes.pomodoro.model.PomodoroListener;
import com.sistemaestudantes.pomodoro.model.PomodoroModel;
import com.sistemaestudantes.pomodoro.model.PomodoroState;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Painel customizado que renderiza o relógio Pomodoro com anel circular de progresso,
 * contagem regressiva em formato MM:SS e indicador visual do estado corrente.
 */
public class CircularTimerPanel extends JPanel implements PomodoroListener {

    private final PomodoroModel model;

    // Dimensões recomendadas
    private static final int PREFERRED_SIZE = 340;
    private static final int RING_STROKE_WIDTH = 12;

    // Cores de fundo e trilha
    private static final Color TRACK_COLOR = new Color(55, 62, 75);
    private static final Color BADGE_TEXT_COLOR = Color.WHITE;
    private static final Color TIMER_TEXT_COLOR = new Color(245, 247, 250);
    private static final Color SUBTITLE_COLOR = new Color(160, 170, 185);

    public CircularTimerPanel(PomodoroModel model) {
        this.model = model;
        this.model.addListener(this);
        setPreferredSize(new Dimension(PREFERRED_SIZE, PREFERRED_SIZE));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - (RING_STROKE_WIDTH * 3);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        PomodoroState state = model.getCurrentState();
        Color accentColor = state.getPrimaryColor();

        // 1. Desenhar a trilha de fundo do círculo
        g2.setStroke(new BasicStroke(RING_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TRACK_COLOR);
        g2.drawOval(x, y, size, size);

        // 2. Desenhar o arco de progresso decorrido (sentido horário a partir do topo)
        double progress = model.getProgressFraction();
        int angle = (int) Math.round(progress * 360.0);
        if (angle > 0) {
            g2.setColor(accentColor);
            // Inicia em 90 graus (topo) e avança no sentido horário (-angle)
            g2.drawArc(x, y, size, size, 90, -angle);
        }

        // 3. Desenhar Badge de Estado ("FOCO" ou "DESCANSO")
        String badgeText = (state == PomodoroState.FOCUS ? "● MODO FOCO (25 MIN)" : "● PAUSA CURTA (5 MIN)");
        Font badgeFont = new Font("Segoe UI", Font.BOLD, 12);
        g2.setFont(badgeFont);
        FontMetrics badgeFm = g2.getFontMetrics();
        int badgeTextWidth = badgeFm.stringWidth(badgeText);
        int badgeWidth = badgeTextWidth + 24;
        int badgeHeight = 24;
        int badgeX = (width - badgeWidth) / 2;
        int badgeY = y + (size / 4) - 10;

        // Fundo do badge translúcido com a cor do estado
        Color badgeBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 45);
        g2.setColor(badgeBg);
        g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 16, 16);
        g2.setColor(accentColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 16, 16);

        // Texto do badge
        g2.setColor(BADGE_TEXT_COLOR);
        int badgeTextY = badgeY + ((badgeHeight - badgeFm.getHeight()) / 2) + badgeFm.getAscent();
        g2.drawString(badgeText, badgeX + 12, badgeTextY);

        // 4. Desenhar o Tempo Restante em Formato MM:SS
        String timeText = model.getFormattedTime();
        Font timerFont = new Font("Segoe UI", Font.BOLD, 54);
        g2.setFont(timerFont);
        FontMetrics timerFm = g2.getFontMetrics();
        int timeWidth = timerFm.stringWidth(timeText);
        int timeX = (width - timeWidth) / 2;
        int timeY = y + (size / 2) + (timerFm.getAscent() / 3) + 5;

        g2.setColor(TIMER_TEXT_COLOR);
        g2.drawString(timeText, timeX, timeY);

        // 5. Desenhar Mensagem / Subtítulo motivacional
        String subText = model.isRunning() 
                ? (state == PomodoroState.FOCUS ? "Mantenha o foco total!" : "Relaxe a mente!")
                : "Clique em Iniciar";
        Font subFont = new Font("Segoe UI", Font.PLAIN, 13);
        g2.setFont(subFont);
        FontMetrics subFm = g2.getFontMetrics();
        int subWidth = subFm.stringWidth(subText);
        int subX = (width - subWidth) / 2;
        int subY = timeY + 36;

        g2.setColor(SUBTITLE_COLOR);
        g2.drawString(subText, subX, subY);

        g2.dispose();
    }

    @Override
    public void onTick(int remainingSeconds, int totalSeconds, double progress) {
        repaint();
    }

    @Override
    public void onStateChanged(PomodoroState newState, int completedCycles) {
        repaint();
    }

    @Override
    public void onTimerFinished(PomodoroState finishedState) {
        repaint();
    }

    @Override
    public void onRunningStatusChanged(boolean isRunning) {
        repaint();
    }
}
