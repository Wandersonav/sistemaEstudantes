package com.sistemaestudantes.scheduling.analytics;

import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Serviço responsável por calcular e agregar métricas de carga horária e consistência.
 */
public class StudyAnalyticsService {

    private final StudySessionRepository repository;

    public StudyAnalyticsService(StudySessionRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("O repositório de sessões não pode ser nulo.");
        }
        this.repository = repository;
    }

    /**
     * Calcula o consolidado completo de métricas com base nas sessões registradas.
     */
    public StudyMetrics calculateMetrics() {
        return calculateMetricsForDate(LocalDate.now());
    }

    /**
     * Permite calcular métricas para uma data de referência (útil para testes temporais).
     */
    public StudyMetrics calculateMetricsForDate(LocalDate referenceDate) {
        List<StudySession> allSessions = repository.findAll();

        // Filtra sessões consideradas para carga horária (Concluídas ou Em Andamento)
        List<StudySession> effectiveSessions = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.CONCLUIDA || s.getStatus() == SessionStatus.EM_ANDAMENTO)
                .collect(Collectors.toList());

        long totalMinutesAllTime = 0;
        for (StudySession s : effectiveSessions) {
            totalMinutesAllTime += s.getDurationMinutes();
        }

        // --- 1. Visão Semanal (Segunda a Domingo) ---
        LocalDate startOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<DayOfWeek, Long> dailyMinutesWeek = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            dailyMinutesWeek.put(d, 0L);
        }

        long totalMinutesWeek = 0;
        for (StudySession s : effectiveSessions) {
            if (s.getStartTime() != null) {
                LocalDate sessionDate = s.getStartTime().toLocalDate();
                if (!sessionDate.isBefore(startOfWeek) && !sessionDate.isAfter(endOfWeek)) {
                    DayOfWeek dow = sessionDate.getDayOfWeek();
                    dailyMinutesWeek.put(dow, dailyMinutesWeek.get(dow) + s.getDurationMinutes());
                    totalMinutesWeek += s.getDurationMinutes();
                }
            }
        }

        // --- 2. Visão Mensal (Mês da data de referência) ---
        int currentMonth = referenceDate.getMonthValue();
        int currentYear = referenceDate.getYear();

        Map<String, Long> subjectMinutesMonth = new HashMap<>();
        long totalMinutesMonth = 0;

        for (StudySession s : effectiveSessions) {
            if (s.getStartTime() != null) {
                LocalDate sessionDate = s.getStartTime().toLocalDate();
                if (sessionDate.getMonthValue() == currentMonth && sessionDate.getYear() == currentYear) {
                    String subject = s.getSubjectName() != null ? s.getSubjectName() : "Outros";
                    subjectMinutesMonth.put(subject, subjectMinutesMonth.getOrDefault(subject, 0L) + s.getDurationMinutes());
                    totalMinutesMonth += s.getDurationMinutes();
                }
            }
        }

        Map<String, Double> subjectPercentageMonth = new HashMap<>();
        if (totalMinutesMonth > 0) {
            for (Map.Entry<String, Long> entry : subjectMinutesMonth.entrySet()) {
                double pct = (entry.getValue() * 100.0) / totalMinutesMonth;
                subjectPercentageMonth.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
            }
        }

        // --- 3. Visão Anual e Heatmap de Consistência (Últimos 365 dias) ---
        LocalDate oneYearAgo = referenceDate.minusDays(364);
        Map<LocalDate, Long> dailyMinutesPastYear = new TreeMap<>();
        long totalMinutesYear = 0;

        for (StudySession s : effectiveSessions) {
            if (s.getStartTime() != null) {
                LocalDate sDate = s.getStartTime().toLocalDate();
                if (!sDate.isBefore(oneYearAgo) && !sDate.isAfter(referenceDate)) {
                    dailyMinutesPastYear.put(sDate, dailyMinutesPastYear.getOrDefault(sDate, 0L) + s.getDurationMinutes());
                }
                if (sDate.getYear() == currentYear) {
                    totalMinutesYear += s.getDurationMinutes();
                }
            }
        }

        int activeDaysYear = dailyMinutesPastYear.size();

        // Cálculo de Streaks (dias consecutivos)
        int currentStreak = calculateCurrentStreak(dailyMinutesPastYear, referenceDate);
        int longestStreak = calculateLongestStreak(dailyMinutesPastYear);

        return new StudyMetrics(
                totalMinutesAllTime,
                allSessions.size(),
                totalMinutesWeek,
                dailyMinutesWeek,
                totalMinutesMonth,
                subjectMinutesMonth,
                subjectPercentageMonth,
                totalMinutesYear,
                activeDaysYear,
                currentStreak,
                longestStreak,
                dailyMinutesPastYear
        );
    }

    private int calculateCurrentStreak(Map<LocalDate, Long> dailyActivity, LocalDate referenceDate) {
        if (dailyActivity.isEmpty()) return 0;

        // Se estudou hoje, a contagem começa hoje; senão, se estudou ontem, começa de ontem
        LocalDate checkDate = referenceDate;
        if (!dailyActivity.containsKey(checkDate)) {
            checkDate = referenceDate.minusDays(1);
            if (!dailyActivity.containsKey(checkDate)) {
                return 0;
            }
        }

        int streak = 0;
        while (dailyActivity.containsKey(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }
        return streak;
    }

    private int calculateLongestStreak(Map<LocalDate, Long> dailyActivity) {
        if (dailyActivity.isEmpty()) return 0;

        List<LocalDate> sortedDates = new ArrayList<>(dailyActivity.keySet());
        Collections.sort(sortedDates);

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).equals(sortedDates.get(i - 1).plusDays(1))) {
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else if (!sortedDates.get(i).equals(sortedDates.get(i - 1))) {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }
}
