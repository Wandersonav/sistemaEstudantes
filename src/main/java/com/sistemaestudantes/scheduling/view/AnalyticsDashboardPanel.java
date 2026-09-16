package com.sistemaestudantes.scheduling.view;

import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.analytics.StudyMetrics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/**
 * Painel analítico de carga horária com Visão Semanal, Mensal e Anual (Heatmap).
 */
public class AnalyticsDashboardPanel extends JPanel {

    private final StudyAnalyticsService analyticsService;

    // Componentes de KPI
    private JLabel totalHoursKpi;
    private JLabel weekHoursKpi;
    private JLabel monthHoursKpi;
    private JLabel streakKpi;

    // Painéis dinâmicos
    private JPanel weeklyBarsPanel;
    private JPanel monthlyDistributionPanel;
    private HeatmapPanel heatmapPanel;
    private JLabel annualSummaryLabel;

    private static final Color CARD_BG = new Color(30, 34, 43);
    private static final Color TEXT_TITLE = new Color(240, 242, 245);
    private static final Color TEXT_MUTED = new Color(150, 160, 175);
    private static final Color ACCENT_BLUE = new Color(66, 133, 244);
    private static final Color ACCENT_GREEN = new Color(46, 204, 113);
    private static final Color ACCENT_ORANGE = new Color(243, 156, 18);

    public AnalyticsDashboardPanel(StudyAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;

        setLayout(new BorderLayout());
        setBackground(new Color(22, 24, 30));

        initUI();
        refreshMetrics();
    }

    private void initUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(22, 24, 30));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // Cabeçalho
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Painel Analítico de Produtividade e Estudos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_TITLE);

        JButton refreshBtn = new JButton("↻ Atualizar Métricas");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshMetrics());

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        contentPanel.add(headerPanel);
        contentPanel.add(Box.createVerticalStrut(18));

        // 1. Linha de Cartões de KPI
        JPanel kpiRow = createKpiRow();
        contentPanel.add(kpiRow);
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Linha Central: Visão Semanal e Visão Mensal lado a lado
        JPanel midRow = new JPanel(new GridLayout(1, 2, 16, 0));
        midRow.setOpaque(false);

        JPanel weekCard = createSectionCard("📅 Visão Semanal (Progresso Diário)");
        weeklyBarsPanel = new JPanel();
        weeklyBarsPanel.setLayout(new BoxLayout(weeklyBarsPanel, BoxLayout.Y_AXIS));
        weeklyBarsPanel.setOpaque(false);
        weekCard.add(weeklyBarsPanel, BorderLayout.CENTER);

        JPanel monthCard = createSectionCard("📊 Visão Mensal (Divisão por Disciplina)");
        monthlyDistributionPanel = new JPanel();
        monthlyDistributionPanel.setLayout(new BoxLayout(monthlyDistributionPanel, BoxLayout.Y_AXIS));
        monthlyDistributionPanel.setOpaque(false);
        monthCard.add(monthlyDistributionPanel, BorderLayout.CENTER);

        midRow.add(weekCard);
        midRow.add(monthCard);
        contentPanel.add(midRow);
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Visão Anual e Heatmap de Consistência
        JPanel annualCard = createSectionCard("🔥 Visão Anual & Mapa de Consistência de Estudos (Últimos 365 Dias)");
        JPanel annualInner = new JPanel(new BorderLayout());
        annualInner.setOpaque(false);

        annualSummaryLabel = new JLabel("Consistência anual de estudos:", SwingConstants.LEFT);
        annualSummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        annualSummaryLabel.setForeground(TEXT_MUTED);
        annualSummaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 0));

        heatmapPanel = new HeatmapPanel();
        annualInner.add(annualSummaryLabel, BorderLayout.NORTH);
        annualInner.add(heatmapPanel, BorderLayout.CENTER);
        annualCard.add(annualInner, BorderLayout.CENTER);

        contentPanel.add(annualCard);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        totalHoursKpi = new JLabel("0h", SwingConstants.CENTER);
        weekHoursKpi = new JLabel("0h", SwingConstants.CENTER);
        monthHoursKpi = new JLabel("0h", SwingConstants.CENTER);
        streakKpi = new JLabel("0 dias", SwingConstants.CENTER);

        row.add(createKpiCard("⏱️ Total Acumulado", totalHoursKpi, ACCENT_BLUE));
        row.add(createKpiCard("📅 Esta Semana", weekHoursKpi, ACCENT_GREEN));
        row.add(createKpiCard("📆 Este Mês", monthHoursKpi, ACCENT_ORANGE));
        row.add(createKpiCard("🔥 Sequência (Streak)", streakKpi, new Color(231, 76, 60)));

        return row;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 50, 62), 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 50, 62), 1, true),
                BorderFactory.createEmptyBorder(16, 18, 18, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(TEXT_TITLE);

        card.add(titleLbl, BorderLayout.NORTH);
        return card;
    }

    public void refreshMetrics() {
        StudyMetrics m = analyticsService.calculateMetrics();

        // Atualizar KPIs
        totalHoursKpi.setText(String.format("%.1fh", m.getHoursAllTime()));
        weekHoursKpi.setText(String.format("%.1fh", m.getHoursThisWeek()));
        monthHoursKpi.setText(String.format("%.1fh", m.getHoursThisMonth()));
        streakKpi.setText(m.getCurrentStreakDays() + " dias");

        // Atualizar Visão Semanal
        updateWeeklyView(m);

        // Atualizar Visão Mensal
        updateMonthlyView(m);

        // Atualizar Heatmap e Resumo Anual
        heatmapPanel.setData(m.getDailyMinutesPastYear());
        annualSummaryLabel.setText(String.format(
                "Total no ano: <b>%.1fh</b> • Dias ativos: <b>%d</b> • Maior sequência histórica: <b>%d dias seguidos</b>",
                m.getHoursThisYear(), m.getActiveDaysThisYear(), m.getLongestStreakDays()
        ));
        annualSummaryLabel.setText("<html>" + annualSummaryLabel.getText() + "</html>");
    }

    private void updateWeeklyView(StudyMetrics m) {
        weeklyBarsPanel.removeAll();
        Map<DayOfWeek, Long> daily = m.getDailyMinutesThisWeek();
        long maxMinutes = daily.values().stream().max(Long::compare).orElse(60L);
        if (maxMinutes == 0) maxMinutes = 60L;

        Locale ptBR = new Locale("pt", "BR");
        for (DayOfWeek dow : DayOfWeek.values()) {
            long minutes = daily.getOrDefault(dow, 0L);
            String dayName = dow.getDisplayName(TextStyle.SHORT, ptBR);
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel dayLbl = new JLabel(dayName);
            dayLbl.setPreferredSize(new Dimension(38, 20));
            dayLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dayLbl.setForeground(TEXT_MUTED);

            JProgressBar bar = new JProgressBar(0, (int) maxMinutes);
            bar.setValue((int) minutes);
            bar.setForeground(minutes > 0 ? ACCENT_GREEN : new Color(60, 66, 80));
            bar.setBackground(new Color(40, 45, 56));
            bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(140, 14));

            long h = minutes / 60;
            long min = minutes % 60;
            String timeStr = (h > 0 ? h + "h " : "") + (min > 0 ? min + "m" : (h == 0 ? "0m" : ""));
            JLabel valLbl = new JLabel(timeStr, SwingConstants.RIGHT);
            valLbl.setPreferredSize(new Dimension(55, 20));
            valLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            valLbl.setForeground(TEXT_TITLE);

            row.add(dayLbl, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            row.add(valLbl, BorderLayout.EAST);

            weeklyBarsPanel.add(row);
            weeklyBarsPanel.add(Box.createVerticalStrut(6));
        }
        weeklyBarsPanel.revalidate();
        weeklyBarsPanel.repaint();
    }

    private void updateMonthlyView(StudyMetrics m) {
        monthlyDistributionPanel.removeAll();
        Map<String, Long> subMinutes = m.getSubjectMinutesThisMonth();
        Map<String, Double> subPercentages = m.getSubjectPercentageThisMonth();

        if (subMinutes.isEmpty()) {
            JLabel emptyLbl = new JLabel("Nenhum estudo registrado neste mês ainda.", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            emptyLbl.setForeground(TEXT_MUTED);
            emptyLbl.setAlignmentX(CENTER_ALIGNMENT);
            monthlyDistributionPanel.add(Box.createVerticalStrut(20));
            monthlyDistributionPanel.add(emptyLbl);
        } else {
            for (Map.Entry<String, Long> entry : subMinutes.entrySet()) {
                String subject = entry.getKey();
                long minutes = entry.getValue();
                double pct = subPercentages.getOrDefault(subject, 0.0);

                JPanel row = new JPanel(new BorderLayout(8, 2));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

                JLabel subLbl = new JLabel(subject);
                subLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                subLbl.setForeground(TEXT_TITLE);

                long h = minutes / 60;
                long min = minutes % 60;
                String timeStr = String.format("%.1f%% (%s)", pct, (h > 0 ? h + "h " : "") + min + "m");
                JLabel pctLbl = new JLabel(timeStr, SwingConstants.RIGHT);
                pctLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                pctLbl.setForeground(TEXT_MUTED);

                JProgressBar bar = new JProgressBar(0, 100);
                bar.setValue((int) Math.round(pct));
                bar.setForeground(ACCENT_BLUE);
                bar.setBackground(new Color(40, 45, 56));
                bar.setBorderPainted(false);
                bar.setPreferredSize(new Dimension(200, 10));

                JPanel top = new JPanel(new BorderLayout());
                top.setOpaque(false);
                top.add(subLbl, BorderLayout.WEST);
                top.add(pctLbl, BorderLayout.EAST);

                row.add(top, BorderLayout.NORTH);
                row.add(bar, BorderLayout.CENTER);

                monthlyDistributionPanel.add(row);
                monthlyDistributionPanel.add(Box.createVerticalStrut(8));
            }
        }
        monthlyDistributionPanel.revalidate();
        monthlyDistributionPanel.repaint();
    }
}
