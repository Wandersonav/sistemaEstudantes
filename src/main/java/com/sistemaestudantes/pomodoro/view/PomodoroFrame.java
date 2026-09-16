package com.sistemaestudantes.pomodoro.view;

import com.sistemaestudantes.pomodoro.controller.PomodoroController;
import com.sistemaestudantes.pomodoro.model.PomodoroListener;
import com.sistemaestudantes.pomodoro.model.PomodoroModel;
import com.sistemaestudantes.pomodoro.model.PomodoroState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Janela principal da aplicação Pomodoro.
 * Proporciona uma interface visual moderna, intuitiva e responsiva.
 */
public class PomodoroFrame extends JFrame implements PomodoroListener {

    private final PomodoroModel model;
    private final PomodoroController controller;

    // Componentes de interface
    private CircularTimerPanel circularTimerPanel;
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel cyclesLabel;
    private JButton playPauseButton;
    private JButton resetButton;
    private JButton skipButton;
    private JPanel cycleDotsPanel;

    // Paleta de Cores
    private static final Color BACKGROUND_COLOR = new Color(24, 26, 32);
    private static final Color CARD_COLOR = new Color(32, 36, 45);
    private static final Color TEXT_PRIMARY = new Color(240, 242, 245);
    private static final Color TEXT_SECONDARY = new Color(150, 158, 170);
    private static final Color BUTTON_SECONDARY_BG = new Color(45, 52, 65);
    private static final Color BUTTON_SECONDARY_HOVER = new Color(55, 64, 80);

    public PomodoroFrame(PomodoroModel model, PomodoroController controller) {
        this.model = model;
        this.controller = controller;

        this.model.addListener(this);

        initUI();
        updateDisplay();
    }

    private void initUI() {
        setTitle("Sistema de Estudantes — Pomodoro Timer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 640);
        setMinimumSize(new Dimension(400, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        setLayout(new BorderLayout(0, 0));

        // Painel Principal Central (Card)
        JPanel mainCard = new JPanel();
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));
        mainCard.setBackground(CARD_COLOR);
        mainCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // 1. Cabeçalho
        JPanel headerPanel = createHeaderPanel();
        mainCard.add(headerPanel);
        mainCard.add(Box.createVerticalStrut(16));

        // 2. Relógio Circular
        circularTimerPanel = new CircularTimerPanel(model);
        circularTimerPanel.setAlignmentX(CENTER_ALIGNMENT);
        mainCard.add(circularTimerPanel);
        mainCard.add(Box.createVerticalStrut(12));

        // 3. Indicador de Ciclos
        JPanel cyclesSection = createCyclesSection();
        mainCard.add(cyclesSection);
        mainCard.add(Box.createVerticalStrut(20));

        // 4. Painel de Controles
        JPanel controlsPanel = createControlsPanel();
        mainCard.add(controlsPanel);
        mainCard.add(Box.createVerticalStrut(16));

        // 5. Rodapé informativo
        JLabel footerLabel = new JLabel("25 min de foco seguidos de 5 min de descanso.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerLabel.setForeground(TEXT_SECONDARY);
        footerLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainCard.add(footerLabel);

        add(mainCard, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(CENTER_ALIGNMENT);

        titleLabel = new JLabel("Técnica Pomodoro", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        descriptionLabel = new JLabel("Gerenciamento de Foco e Produtividade", SwingConstants.CENTER);
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionLabel.setForeground(TEXT_SECONDARY);
        descriptionLabel.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(descriptionLabel);

        return panel;
    }

    private JPanel createCyclesSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(CENTER_ALIGNMENT);

        cyclesLabel = new JLabel("Ciclos de foco concluídos: 0", SwingConstants.CENTER);
        cyclesLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cyclesLabel.setForeground(TEXT_SECONDARY);
        cyclesLabel.setAlignmentX(CENTER_ALIGNMENT);

        cycleDotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        cycleDotsPanel.setOpaque(false);
        cycleDotsPanel.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(cyclesLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(cycleDotsPanel);

        return panel;
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(CENTER_ALIGNMENT);

        // Botão Reiniciar
        resetButton = createStyledButton("↺ Reiniciar", BUTTON_SECONDARY_BG, BUTTON_SECONDARY_HOVER, TEXT_PRIMARY, 110, 44);
        resetButton.addActionListener(e -> controller.reset());

        // Botão Iniciar / Pausar
        playPauseButton = createStyledButton("▶ Iniciar", model.getCurrentState().getPrimaryColor(),
                model.getCurrentState().getDarkAccentColor(), Color.WHITE, 130, 48);
        playPauseButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        playPauseButton.addActionListener(e -> controller.togglePlayPause());

        // Botão Pular Etapa
        skipButton = createStyledButton("⏭ Pular", BUTTON_SECONDARY_BG, BUTTON_SECONDARY_HOVER, TEXT_PRIMARY, 100, 44);
        skipButton.addActionListener(e -> controller.skip());

        panel.add(resetButton);
        panel.add(playPauseButton);
        panel.add(skipButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color bg, Color hoverBg, Color fg, int width, int height) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restaura cor de acordo com o estado do botão
                if (button == playPauseButton) {
                    button.setBackground(model.getCurrentState().getPrimaryColor());
                } else {
                    button.setBackground(bg);
                }
            }
        });

        return button;
    }

    private void updateDisplay() {
        PomodoroState state = model.getCurrentState();

        // Atualizar textos e descrição do estado
        titleLabel.setText(state.getTitle());
        descriptionLabel.setText(state.getDescription());

        // Atualizar Botão Iniciar/Pausar
        if (model.isRunning()) {
            playPauseButton.setText("⏸ Pausar");
        } else {
            playPauseButton.setText("▶ Iniciar");
        }
        playPauseButton.setBackground(state.getPrimaryColor());

        // Atualizar Contador de Ciclos
        int cycles = model.getCompletedFocusCycles();
        cyclesLabel.setText("Ciclos de foco concluídos: " + cycles);

        // Atualizar Marcadores Visuais dos Ciclos (Badges em grupos de 4)
        cycleDotsPanel.removeAll();
        int activeInSet = cycles % 4;
        if (cycles > 0 && activeInSet == 0) activeInSet = 4;

        for (int i = 1; i <= 4; i++) {
            JLabel dot = new JLabel((i <= activeInSet && cycles > 0) ? "●" : "○");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            dot.setForeground((i <= activeInSet && cycles > 0) ? state.getPrimaryColor() : TEXT_SECONDARY);
            cycleDotsPanel.add(dot);
        }
        cycleDotsPanel.revalidate();
        cycleDotsPanel.repaint();
    }

    // PomodoroListener Callbacks
    @Override
    public void onTick(int remainingSeconds, int totalSeconds, double progress) {
        // Atualizações contínuas de tempo são desenhadas pelo CircularTimerPanel
    }

    @Override
    public void onStateChanged(PomodoroState newState, int completedCycles) {
        updateDisplay();
    }

    @Override
    public void onTimerFinished(PomodoroState finishedState) {
        updateDisplay();
    }

    @Override
    public void onRunningStatusChanged(boolean isRunning) {
        updateDisplay();
    }
}
