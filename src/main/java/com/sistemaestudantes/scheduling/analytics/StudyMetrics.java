package com.sistemaestudantes.scheduling.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/**
 * Objeto imutável contendo as métricas agregadas de carga horária e consistência de estudo.
 */
public class StudyMetrics {

    // Métricas Gerais
    private final long totalMinutesAllTime;
    private final int totalSessionsCount;

    // Visão Semanal
    private final long totalMinutesThisWeek;
    private final Map<DayOfWeek, Long> dailyMinutesThisWeek;

    // Visão Mensal
    private final long totalMinutesThisMonth;
    private final Map<String, Long> subjectMinutesThisMonth;
    private final Map<String, Double> subjectPercentageThisMonth;

    // Visão Anual e Consistência
    private final long totalMinutesThisYear;
    private final int activeDaysThisYear;
    private final int currentStreakDays;
    private final int longestStreakDays;
    private final Map<LocalDate, Long> dailyMinutesPastYear;

    public StudyMetrics(long totalMinutesAllTime, int totalSessionsCount,
                        long totalMinutesThisWeek, Map<DayOfWeek, Long> dailyMinutesThisWeek,
                        long totalMinutesThisMonth, Map<String, Long> subjectMinutesThisMonth,
                        Map<String, Double> subjectPercentageThisMonth,
                        long totalMinutesThisYear, int activeDaysThisYear,
                        int currentStreakDays, int longestStreakDays,
                        Map<LocalDate, Long> dailyMinutesPastYear) {
        this.totalMinutesAllTime = totalMinutesAllTime;
        this.totalSessionsCount = totalSessionsCount;
        this.totalMinutesThisWeek = totalMinutesThisWeek;
        this.dailyMinutesThisWeek = Collections.unmodifiableMap(dailyMinutesThisWeek);
        this.totalMinutesThisMonth = totalMinutesThisMonth;
        this.subjectMinutesThisMonth = Collections.unmodifiableMap(subjectMinutesThisMonth);
        this.subjectPercentageThisMonth = Collections.unmodifiableMap(subjectPercentageThisMonth);
        this.totalMinutesThisYear = totalMinutesThisYear;
        this.activeDaysThisYear = activeDaysThisYear;
        this.currentStreakDays = currentStreakDays;
        this.longestStreakDays = longestStreakDays;
        this.dailyMinutesPastYear = Collections.unmodifiableMap(dailyMinutesPastYear);
    }

    public double getHoursThisWeek() {
        return totalMinutesThisWeek / 60.0;
    }

    public double getHoursThisMonth() {
        return totalMinutesThisMonth / 60.0;
    }

    public double getHoursThisYear() {
        return totalMinutesThisYear / 60.0;
    }

    public double getHoursAllTime() {
        return totalMinutesAllTime / 60.0;
    }

    // Getters
    public long getTotalMinutesAllTime() {
        return totalMinutesAllTime;
    }

    public int getTotalSessionsCount() {
        return totalSessionsCount;
    }

    public long getTotalMinutesThisWeek() {
        return totalMinutesThisWeek;
    }

    public Map<DayOfWeek, Long> getDailyMinutesThisWeek() {
        return dailyMinutesThisWeek;
    }

    public long getTotalMinutesThisMonth() {
        return totalMinutesThisMonth;
    }

    public Map<String, Long> getSubjectMinutesThisMonth() {
        return subjectMinutesThisMonth;
    }

    public Map<String, Double> getSubjectPercentageThisMonth() {
        return subjectPercentageThisMonth;
    }

    public long getTotalMinutesThisYear() {
        return totalMinutesThisYear;
    }

    public int getActiveDaysThisYear() {
        return activeDaysThisYear;
    }

    public int getCurrentStreakDays() {
        return currentStreakDays;
    }

    public int getLongestStreakDays() {
        return longestStreakDays;
    }

    public Map<LocalDate, Long> getDailyMinutesPastYear() {
        return dailyMinutesPastYear;
    }
}
