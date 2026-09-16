package com.sistemaestudantes.scheduling.view;

import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.domain.Subject;
import com.sistemaestudantes.scheduling.domain.SyncStatus;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Painel de agendamento e gerenciamento de sessões com sincronização assíncrona ao Google Calendar via MCP.
 */
public class ScheduleManagementPanel extends JPanel {

    private final StudySessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final McpClientService mcpClientService;
    private final Runnable onDataChangedCallback;
    private Consumer<StudySession> onStartPomodoroCallback;

    // Campos do formulário
    private JComboBox<Subject> subjectComboBox;
    private JTextField topicField;
    private JTextField dateField;
    private JTextField startTimeField;
    private JComboBox<String> durationComboBox;
    private JComboBox<ActivityType> activityTypeComboBox;
    private JComboBox<SessionStatus> statusComboBox;
    private JCheckBox syncMcpCheckBox;
    private JTextField notesField;

    // Componentes de tabela
    private JTable sessionsTable;
    private DefaultTableModel tableModel;
    private JLabel statusFeedbackLabel;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color CARD_BG = new Color(30, 34, 43);
    private static final Color TEXT_TITLE = new Color(240, 242, 245);
    private static final Color TEXT_MUTED = new Color(150, 160, 175);
    private static final Color PRIMARY_ACCENT = new Color(66, 133, 244);

    public ScheduleManagementPanel(StudySessionRepository sessionRepository,
                                   SubjectRepository subjectRepository,
                                   McpClientService mcpClientService,
                                   Runnable onDataChangedCallback) {
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.mcpClientService = mcpClientService;
        this.onDataChangedCallback = onDataChangedCallback;

        // Ouvinte de eventos MCP para atualizar a tabela em tempo real na EDT
        this.mcpClientService.addSyncListener((session, result) -> {
            SwingUtilities.invokeLater(() -> {
                loadTableData();
                if (result.isSuccess()) {
                    statusFeedbackLabel.setText("✔ MCP: Evento '" + session.getSubjectName() + "' sincronizado no Google Calendar.");
                    statusFeedbackLabel.setForeground(new Color(46, 204, 113));
                } else {
                    statusFeedbackLabel.setText("⚠ MCP Fallback: " + result.getMessage() + " (Dados salvos localmente).");
                    statusFeedbackLabel.setForeground(new Color(235, 87, 87));
                }
                if (onDataChangedCallback != null) {
                    onDataChangedCallback.run();
                }
            });
        });

        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(22, 24, 30));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        initUI();
        loadTableData();
    }

    public void setOnStartPomodoroCallback(Consumer<StudySession> callback) {
        this.onStartPomodoroCallback = callback;
    }

    private void initUI() {
        // 1. Painel Superior: Formulário de Agendamento
        JPanel formCard = createFormCard();
        add(formCard, BorderLayout.NORTH);

        // 2. Painel Central: Tabela de Sessões Agendadas
        JPanel tableCard = createTableCard();
        add(tableCard, BorderLayout.CENTER);

        // 3. Painel Inferior: Barra de Ações Rápidas e Feedback MCP
        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createFormCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 50, 62), 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JLabel title = new JLabel("📅 Novo Bloco de Estudo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_TITLE);
        card.add(title, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: Matéria / Tópico
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        formGrid.add(createFieldLabel("Disciplina / Matéria:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.65;
        formGrid.add(createFieldLabel("Tópico ou Assunto:"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        List<Subject> subjects = subjectRepository.findAll();
        subjectComboBox = new JComboBox<>(subjects.toArray(new Subject[0]));
        formGrid.add(subjectComboBox, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        topicField = new JTextField();
        formGrid.add(topicField, gbc);

        // Linha 2: Data / Início / Duração / Tipo / Status
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        subRow.setOpaque(false);

        LocalDate today = LocalDate.now();
        dateField = new JTextField(today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 8);
        LocalTime now = LocalTime.now().plusMinutes(5);
        startTimeField = new JTextField(String.format("%02d:%02d", now.getHour(), (now.getMinute() / 5) * 5), 5);

        durationComboBox = new JComboBox<>(new String[]{"25 min (Pomodoro)", "50 min", "60 min", "90 min", "120 min"});
        activityTypeComboBox = new JComboBox<>(ActivityType.values());
        statusComboBox = new JComboBox<>(SessionStatus.values());

        subRow.add(createMiniLabeledComp("Data (DD/MM/AAAA):", dateField));
        subRow.add(createMiniLabeledComp("Horário Início:", startTimeField));
        subRow.add(createMiniLabeledComp("Duração:", durationComboBox));
        subRow.add(createMiniLabeledComp("Tipo de Atividade:", activityTypeComboBox));
        subRow.add(createMiniLabeledComp("Status:", statusComboBox));

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formGrid.add(subRow, gbc);

        // Linha 3: Observações & Opção MCP
        JPanel optionsRow = new JPanel(new BorderLayout(10, 0));
        optionsRow.setOpaque(false);

        notesField = new JTextField();
        notesField.setToolTipText("Observações adicionais ou metas desta sessão");
        optionsRow.add(createMiniLabeledComp("Anotações da Sessão:", notesField), BorderLayout.CENTER);

        syncMcpCheckBox = new JCheckBox("Sincronizar no Google Calendar (MCP)", true);
        syncMcpCheckBox.setFont(new Font("Segoe UI", Font.BOLD, 12));
        syncMcpCheckBox.setForeground(new Color(66, 133, 244));
        syncMcpCheckBox.setOpaque(false);

        JButton saveBtn = new JButton("💾 Agendar Sessão");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(PRIMARY_ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> handleSaveSession());

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        rightActions.setOpaque(false);
        rightActions.add(syncMcpCheckBox);
        rightActions.add(saveBtn);
        optionsRow.add(rightActions, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formGrid.add(optionsRow, gbc);

        card.add(formGrid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMiniLabeledComp(String labelText, Component comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JPanel createTableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 50, 62), 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel title = new JLabel("📋 Sessões de Estudo Registradas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_TITLE);
        card.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Data & Hora", "Disciplina", "Tópico", "Atividade", "Duração", "Status", "Google Calendar (MCP)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        sessionsTable = new JTable(tableModel);
        sessionsTable.setRowHeight(32);
        sessionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Esconder coluna ID
        sessionsTable.getColumnModel().getColumn(0).setMinWidth(0);
        sessionsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        sessionsTable.getColumnModel().getColumn(0).setWidth(0);

        // Estilização customizada das células de Status e Sincronização
        sessionsTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        sessionsTable.getColumnModel().getColumn(7).setCellRenderer(new SyncBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(sessionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 50, 62)));
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        statusFeedbackLabel = new JLabel("Status MCP: Conexão pronta e ativa.", SwingConstants.LEFT);
        statusFeedbackLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusFeedbackLabel.setForeground(TEXT_MUTED);
        bar.add(statusFeedbackLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton pomodoroBtn = new JButton("🍅 Iniciar no Pomodoro");
        pomodoroBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pomodoroBtn.setBackground(new Color(235, 87, 87));
        pomodoroBtn.setForeground(Color.WHITE);
        pomodoroBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pomodoroBtn.addActionListener(e -> handleStartPomodoro());

        JButton syncBtn = new JButton("🔄 Sincronizar MCP");
        syncBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        syncBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        syncBtn.addActionListener(e -> handleManualSync());

        JButton completeBtn = new JButton("✔ Marcar Concluída");
        completeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        completeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        completeBtn.addActionListener(e -> handleMarkCompleted());

        JButton deleteBtn = new JButton("🗑 Excluir");
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        deleteBtn.setForeground(new Color(235, 87, 87));
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> handleDeleteSession());

        actions.add(pomodoroBtn);
        actions.add(syncBtn);
        actions.add(completeBtn);
        actions.add(deleteBtn);

        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void handleSaveSession() {
        Subject selectedSubject = (Subject) subjectComboBox.getSelectedItem();
        if (selectedSubject == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma disciplina.", "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String topic = topicField.getText().trim();
        if (topic.isBlank()) {
            JOptionPane.showMessageDialog(this, "Informe o tópico ou conteúdo de estudo.", "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Formato de data inválido. Use DD/MM/AAAA (ex: " + LocalDate.now().format(DATE_FMT) + ")", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(startTimeField.getText().trim(), TIME_FMT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Formato de horário inválido. Use HH:MM (ex: 14:30)", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int durationMin = parseDuration((String) durationComboBox.getSelectedItem());
        LocalDateTime start = date.atTime(time);
        LocalDateTime end = start.plusMinutes(durationMin);

        ActivityType actType = (ActivityType) activityTypeComboBox.getSelectedItem();
        SessionStatus status = (SessionStatus) statusComboBox.getSelectedItem();
        String notes = notesField.getText().trim();

        StudySession session = new StudySession(
                selectedSubject.getId(),
                selectedSubject.getName(),
                topic,
                start,
                end,
                actType,
                status,
                notes
        );

        // 1. Salva localmente
        sessionRepository.save(session);
        loadTableData();

        // 2. Se marcado para sincronização MCP, despacha assincronamente
        if (syncMcpCheckBox.isSelected()) {
            statusFeedbackLabel.setText("⏳ MCP: Sincronizando '" + selectedSubject.getName() + "' com Google Calendar...");
            statusFeedbackLabel.setForeground(new Color(243, 156, 18));
            mcpClientService.syncSessionAsync(session);
        }

        // Limpa campos
        topicField.setText("");
        notesField.setText("");

        if (onDataChangedCallback != null) {
            onDataChangedCallback.run();
        }
    }

    private void handleStartPomodoro() {
        StudySession selected = getSelectedSession();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma sessão na tabela para iniciar no Pomodoro.", "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        selected.setStatus(SessionStatus.EM_ANDAMENTO);
        sessionRepository.save(selected);
        loadTableData();

        if (onStartPomodoroCallback != null) {
            onStartPomodoroCallback.accept(selected);
        }
    }

    private void handleManualSync() {
        StudySession selected = getSelectedSession();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma sessão para sincronizar via MCP.", "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        statusFeedbackLabel.setText("⏳ MCP: Sincronizando sessão selecionada...");
        statusFeedbackLabel.setForeground(new Color(243, 156, 18));
        mcpClientService.syncSessionAsync(selected);
    }

    private void handleMarkCompleted() {
        StudySession selected = getSelectedSession();
        if (selected == null) return;

        selected.setStatus(SessionStatus.CONCLUIDA);
        sessionRepository.save(selected);
        loadTableData();

        if (onDataChangedCallback != null) {
            onDataChangedCallback.run();
        }
    }

    private void handleDeleteSession() {
        StudySession selected = getSelectedSession();
        if (selected == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente remover a sessão de '" + selected.getSubjectName() + "'?",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            sessionRepository.deleteById(selected.getId());
            loadTableData();
            if (onDataChangedCallback != null) {
                onDataChangedCallback.run();
            }
        }
    }

    private StudySession getSelectedSession() {
        int selectedRow = sessionsTable.getSelectedRow();
        if (selectedRow < 0) return null;
        String id = (String) tableModel.getValueAt(selectedRow, 0);
        return sessionRepository.findById(id).orElse(null);
    }

    public void loadTableData() {
        tableModel.setRowCount(0);
        List<StudySession> sessions = sessionRepository.findAll();

        for (StudySession s : sessions) {
            String dateStr = s.getStartTime() != null ? s.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "-";
            String durationStr = s.getDurationMinutes() + " min";
            tableModel.addRow(new Object[]{
                    s.getId(),
                    dateStr,
                    s.getSubjectName(),
                    s.getTopic(),
                    s.getActivityType().getLabel(),
                    durationStr,
                    s.getStatus(),
                    s.getSyncStatus()
            });
        }
    }

    private int parseDuration(String label) {
        if (label == null) return 25;
        if (label.startsWith("25")) return 25;
        if (label.startsWith("50")) return 50;
        if (label.startsWith("60")) return 60;
        if (label.startsWith("90")) return 90;
        if (label.startsWith("120")) return 120;
        return 25;
    }

    // Renderizadores visuais de células (Badges de Status)
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SessionStatus status) {
                label.setText(status.getLabel());
                label.setForeground(status.getColor());
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            return label;
        }
    }

    private static class SyncBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof SyncStatus status) {
                label.setText(status.getLabel());
                label.setForeground(status.getColor());
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            return label;
        }
    }
}
