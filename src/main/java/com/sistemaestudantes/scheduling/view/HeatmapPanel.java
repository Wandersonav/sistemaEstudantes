package com.sistemaestudantes.scheduling.view;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Componente gráfico que renderiza o mapa de consistência de estudos anual (Heatmap estilo GitHub),
 * com células coloridas por intensidade e tooltips interativos com data e tempo estudado.
 */
public class HeatmapPanel extends JPanel {

    private Map<LocalDate, Long> activityData = Collections.emptyMap();
    private final LocalDate endDate;
    private final LocalDate startDate;

    private static final int CELL_SIZE = 13;
    private static final int CELL_GAP = 3;
    private static final int WEEKS = 53;
    private static final int DAYS_IN_WEEK = 7;

    // Cores de intensidade
    private static final Color EMPTY_CELL = new Color(38, 42, 52);
    private static final Color LEVEL_1 = new Color(27, 77, 50);    // 1 - 59 min
    private static final Color LEVEL_2 = new Color(45, 115, 75);   // 60 - 119 min
    private static final Color LEVEL_3 = new Color(46, 175, 100);  // 120 - 179 min
    private static final Color LEVEL_4 = new Color(46, 204, 113);  // 180+ min
    private static final Color BORDER_COLOR = new Color(30, 34, 42);

    private final Map<java.awt.Rectangle, CellInfo> cellBounds = new HashMap<>();

    private static class CellInfo {
        final LocalDate date;
        final long minutes;

        CellInfo(LocalDate date, long minutes) {
            this.date = date;
            this.minutes = minutes;
        }
    }

    public HeatmapPanel() {
        this.endDate = LocalDate.now();
        this.startDate = endDate.minusWeeks(WEEKS - 1).with(DayOfWeek.MONDAY);

        int panelWidth = (WEEKS * (CELL_SIZE + CELL_GAP)) + 60;
        int panelHeight = (DAYS_IN_WEEK * (CELL_SIZE + CELL_GAP)) + 45;
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setOpaque(false);

        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(100);
    }

    public void setData(Map<LocalDate, Long> data) {
        this.activityData = data != null ? data : Collections.emptyMap();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        cellBounds.clear();

        int startX = 35;
        int startY = 20;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(140, 150, 165));

        // Rótulos de dias da semana
        g2.drawString("Seg", 5, startY + CELL_SIZE);
        g2.drawString("Qua", 5, startY + (3 * (CELL_SIZE + CELL_GAP)));
        g2.drawString("Sex", 5, startY + (5 * (CELL_SIZE + CELL_GAP)));

        LocalDate current = startDate;
        int currentMonth = -1;

        for (int week = 0; week < WEEKS; week++) {
            int x = startX + (week * (CELL_SIZE + CELL_GAP));

            for (int day = 0; day < DAYS_IN_WEEK; day++) {
                int y = startY + (day * (CELL_SIZE + CELL_GAP));

                if (!current.isAfter(endDate)) {
                    // Desenha rótulo do mês na primeira ocorrência da semana
                    if (current.getMonthValue() != currentMonth && day == 0) {
                        currentMonth = current.getMonthValue();
                        String monthName = current.format(DateTimeFormatter.ofPattern("MMM", new Locale("pt", "BR")));
                        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                        g2.setColor(new Color(160, 170, 185));
                        g2.drawString(monthName, x, startY - 6);
                    }

                    long minutes = activityData.getOrDefault(current, 0L);
                    Color cellColor = getIntensityColor(minutes);

                    g2.setColor(cellColor);
                    g2.fillRoundRect(x, y, CELL_SIZE, CELL_SIZE, 3, 3);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(x, y, CELL_SIZE, CELL_SIZE, 3, 3);

                    cellBounds.put(new java.awt.Rectangle(x, y, CELL_SIZE, CELL_SIZE), new CellInfo(current, minutes));
                }

                current = current.plusDays(1);
            }
        }

        // Legenda no rodapé
        int legendY = startY + (DAYS_IN_WEEK * (CELL_SIZE + CELL_GAP)) + 12;
        g2.setColor(new Color(140, 150, 165));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString("Menos", startX, legendY + 9);

        int legX = startX + 42;
        Color[] levels = {EMPTY_CELL, LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4};
        for (Color c : levels) {
            g2.setColor(c);
            g2.fillRoundRect(legX, legendY, 10, 10, 2, 2);
            legX += 14;
        }
        g2.setColor(new Color(140, 150, 165));
        g2.drawString("Mais", legX + 2, legendY + 9);

        g2.dispose();
    }

    private Color getIntensityColor(long minutes) {
        if (minutes <= 0) return EMPTY_CELL;
        if (minutes < 60) return LEVEL_1;
        if (minutes < 120) return LEVEL_2;
        if (minutes < 180) return LEVEL_3;
        return LEVEL_4;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (Map.Entry<java.awt.Rectangle, CellInfo> entry : cellBounds.entrySet()) {
            if (entry.getKey().contains(event.getPoint())) {
                CellInfo info = entry.getValue();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy", new Locale("pt", "BR"));
                String dateStr = info.date.format(fmt);
                if (info.minutes == 0) {
                    return String.format("Nenhum estudo registrado em %s", dateStr);
                } else {
                    long h = info.minutes / 60;
                    long m = info.minutes % 60;
                    String timeStr = (h > 0 ? h + "h " : "") + (m > 0 ? m + "m" : (h == 0 ? "0m" : ""));
                    return String.format("<b>%s estudados</b> em %s", timeStr, dateStr);
                }
            }
        }
        return null;
    }
}
