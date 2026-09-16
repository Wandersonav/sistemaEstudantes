package com.sistemaestudantes.view;

import com.sistemaestudantes.pomodoro.controller.PomodoroController;
import com.sistemaestudantes.pomodoro.model.PomodoroModel;
import com.sistemaestudantes.pomodoro.view.CircularTimerPanel;
import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;
import com.sistemaestudantes.scheduling.view.AnalyticsDashboardPanel;
import com.sistemaestudantes.scheduling.view.ScheduleManagementPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
 * Janela principal unificada do ecossistema de gestão de estudos.
 * Integra Planejamento de Horários, Painel Analítico e Cronômetro Pomodoro em abas modernas.
 */
public class MainApplicationFrame extends JFrame {

    private final StudySessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final McpClientService mcpClientService;
    private final StudyAnalyticsService analyticsService;

    // Componentes de navegação
    private JTabbedPane tabbedPane;
    private ScheduleManagementPanel schedulePanel;
    private AnalyticsDashboardPanel analyticsPanel;

    // Pomodoro Integrado
    private PomodoroModel pomodoroModel;
    private PomodoroController pomodoroController;
    private JLabel currentSessionBanner;

    private static final Color BG_DARK = new Color(22, 24, 30);
    private static final Color CARD_BG = new Color(30, 34, 43);
    private static final Color TEXT_PRIMARY = new Color(240, 242, 245);

    public MainApplicationFrame() {
        this.sessionRepository = new InMemoryStudySessionRepository();
        this.subjectRepository = new SubjectRepository();
        this.mcpClientService = new McpClientService(sessionRepository);
        this.analyticsService = new StudyAnalyticsService(sessionRepository);

        initUI();
    }

    private void initUI() {
        setTitle("Sistema de Estudantes — Gestão de Estudos, Horários & Produtividade");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 740);
        setMinimumSize(new Dimension(960, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        setLayout(new BorderLayout());

        // Header Superior
        JPanel appHeader = createAppHeader();
        add(appHeader, BorderLayout.NORTH);

        // Abas Principais
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(BG_DARK);

        // Aba 1: Planejamento & Agenda
        schedulePanel = new ScheduleManagementPanel(
                sessionRepository,
                subjectRepository,
                mcpClientService,
                () -> analyticsPanel.refreshMetrics()
        );
        schedulePanel.setOnStartPomodoroCallback(this::handleStartSessionInPomodoro);
        tabbedPane.addTab("📅 Planejamento & Agenda", schedulePanel);

        // Aba 2: Painel Analítico
        analyticsPanel = new AnalyticsDashboardPanel(analyticsService);
        tabbedPane.addTab("📊 Métricas & Carga Horária", analyticsPanel);

        // Aba 3: Pomodoro Timer
        JPanel pomodoroTab = createPomodoroTab();
        tabbedPane.addTab("🍅 Cronômetro Pomodoro", pomodoroTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createAppHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 20, 25));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 45, 56)),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel("🎓");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel appTitle = new JLabel("Sistema de Estudantes");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        appTitle.setForeground(TEXT_PRIMARY);

        JLabel appSubtitle = new JLabel("•  Planejamento, Métricas & Google Calendar MCP");
        appSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        appSubtitle.setForeground(new Color(150, 160, 175));

        left.add(logo);
        left.add(appTitle);
        left.add(appSubtitle);

        header.add(left, BorderLayout.WEST);
        return header;
    }

    private JPanel createPomodoroTab() {
        pomodoroModel = new PomodoroModel();
        pomodoroController = new PomodoroController(pomodoroModel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel centerCard = new JPanel();
        centerCard.setLayout(new BoxLayout(centerCard, BoxLayout.Y_AXIS));
        centerCard.setBackground(CARD_BG);
        centerCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        centerCard.setMaximumSize(new Dimension(480, 580));

        currentSessionBanner = new JLabel("Sessão Livre de Estudos", SwingConstants.CENTER);
        currentSessionBanner.setFont(new Font("Segoe UI", Font.BOLD, 15));
        currentSessionBanner.setForeground(new Color(66, 133, 244));
        currentSessionBanner.setAlignmentX(CENTER_ALIGNMENT);
        centerCard.add(currentSessionBanner);
        centerCard.add(Box.createVerticalStrut(14));

        CircularTimerPanel timerPanel = new CircularTimerPanel(pomodoroModel);
        timerPanel.setAlignmentX(CENTER_ALIGNMENT);
        centerCard.add(timerPanel);
        centerCard.add(Box.createVerticalStrut(20));

        // Controles do Pomodoro
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(CENTER_ALIGNMENT);

        JButton resetBtn = createStyledButton("↺ Reiniciar", new Color(45, 52, 65), Color.WHITE, 110, 42);
        resetBtn.addActionListener(e -> pomodoroController.reset());

        JButton playPauseBtn = createStyledButton("▶ Iniciar", pomodoroModel.getCurrentState().getPrimaryColor(), Color.WHITE, 130, 44);
        playPauseBtn.addActionListener(e -> pomodoroController.togglePlayPause());

        JButton skipBtn = createStyledButton("⏭ Pular", new Color(45, 52, 65), Color.WHITE, 100, 42);
        skipBtn.addActionListener(e -> pomodoroController.skip());

        pomodoroModel.addListener(new com.sistemaestudantes.pomodoro.model.PomodoroListener() {
            @Override
            public void onTick(int remainingSeconds, int totalSeconds, double progress) {}

            @Override
            public void onStateChanged(com.sistemaestudantes.pomodoro.model.PomodoroState newState, int completedCycles) {
                playPauseBtn.setBackground(newState.getPrimaryColor());
            }

            @Override
            public void onTimerFinished(com.sistemaestudantes.pomodoro.model.PomodoroState finishedState) {
                playPauseBtn.setText("▶ Iniciar");
            }

            @Override
            public void onRunningStatusChanged(boolean isRunning) {
                playPauseBtn.setText(isRunning ? "⏸ Pausar" : "▶ Iniciar");
            }
        });

        controls.add(resetBtn);
        controls.add(playPauseBtn);
        controls.add(skipBtn);
        centerCard.add(controls);

        JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        outer.setOpaque(false);
        outer.add(centerCard);

        panel.add(outer, BorderLayout.CENTER);
        return panel;
    }

    private void handleStartSessionInPomodoro(StudySession session) {
        currentSessionBanner.setText("Sessão em Andamento: " + session.getSubjectName() + " (" + session.getTopic() + ")");
        tabbedPane.setSelectedIndex(2); // Seleciona a aba do Pomodoro
        pomodoroController.reset();
        pomodoroController.start();
    }

    private JButton createStyledButton(String text, Color bg, Color fg, int width, int height) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });

        return button;
    }
}
