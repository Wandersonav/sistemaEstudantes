package com.sistemaestudantes.scheduling;

import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.analytics.StudyMetrics;
import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StudyAnalyticsServiceTest {

    private StudySessionRepository repository;
    private StudyAnalyticsService analyticsService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File tempFile = tempDir.resolve("test-analytics.json").toFile();
        repository = new InMemoryStudySessionRepository(tempFile.getAbsolutePath());
        // Limpar dados iniciais de seed para testes controlados
        for (StudySession s : repository.findAll()) {
            repository.deleteById(s.getId());
        }
        analyticsService = new StudyAnalyticsService(repository);
    }

    @Test
    @DisplayName("Deve calcular métricas semanais e mensais com agregação correta de tempo")
    void testCalculateMetrics() {
        LocalDate refDate = LocalDate.of(2026, 9, 16); // Quarta-feira

        // Sessão 1: Terça-feira (90 min)
        StudySession s1 = new StudySession("subj-1", "Algoritmos", "Recursão",
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 11, 30),
                ActivityType.TEORIA, SessionStatus.CONCLUIDA, "");

        // Sessão 2: Quarta-feira (60 min)
        StudySession s2 = new StudySession("subj-2", "Cálculo", "Matrizes",
                LocalDateTime.of(2026, 9, 16, 14, 0),
                LocalDateTime.of(2026, 9, 16, 15, 0),
                ActivityType.EXERCICIOS, SessionStatus.CONCLUIDA, "");

        // Sessão 3: Planejada (não deve somar na carga horária de estudo executada)
        StudySession s3 = new StudySession("subj-1", "Algoritmos", "Filas",
                LocalDateTime.of(2026, 9, 16, 16, 0),
                LocalDateTime.of(2026, 9, 16, 17, 0),
                ActivityType.EXERCICIOS, SessionStatus.PLANEJADA, "");

        repository.save(s1);
        repository.save(s2);
        repository.save(s3);

        StudyMetrics metrics = analyticsService.calculateMetricsForDate(refDate);

        // 90 + 60 = 150 minutos (2.5 horas)
        assertEquals(150, metrics.getTotalMinutesThisWeek());
        assertEquals(2.5, metrics.getHoursThisWeek(), 0.01);

        // Dias da semana
        assertEquals(90, metrics.getDailyMinutesThisWeek().get(DayOfWeek.TUESDAY));
        assertEquals(60, metrics.getDailyMinutesThisWeek().get(DayOfWeek.WEDNESDAY));
        assertEquals(0, metrics.getDailyMinutesThisWeek().get(DayOfWeek.MONDAY));

        // Divisão por matéria no mês
        assertEquals(90, metrics.getSubjectMinutesThisMonth().get("Algoritmos"));
        assertEquals(60, metrics.getSubjectMinutesThisMonth().get("Cálculo"));
        assertEquals(60.0, metrics.getSubjectPercentageThisMonth().get("Algoritmos"), 0.1);
        assertEquals(40.0, metrics.getSubjectPercentageThisMonth().get("Cálculo"), 0.1);
    }

    @Test
    @DisplayName("Deve calcular sequência de consistência (streak) corretamente")
    void testCalculateStreaks() {
        LocalDate refDate = LocalDate.of(2026, 9, 16);

        // 3 dias seguidos: 14, 15, 16
        addCompletedSession(LocalDate.of(2026, 9, 14), 60);
        addCompletedSession(LocalDate.of(2026, 9, 15), 60);
        addCompletedSession(LocalDate.of(2026, 9, 16), 60);

        StudyMetrics metrics = analyticsService.calculateMetricsForDate(refDate);
        assertEquals(3, metrics.getCurrentStreakDays());
        assertEquals(3, metrics.getLongestStreakDays());
        assertEquals(3, metrics.getActiveDaysThisYear());
    }

    private void addCompletedSession(LocalDate date, int minutes) {
        LocalDateTime start = date.atTime(10, 0);
        StudySession s = new StudySession("subj-1", "Algoritmos", "Tópico",
                start, start.plusMinutes(minutes), ActivityType.TEORIA, SessionStatus.CONCLUIDA, "");
        repository.save(s);
    }
}
