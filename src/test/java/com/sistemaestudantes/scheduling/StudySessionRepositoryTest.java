package com.sistemaestudantes.scheduling;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudySessionRepositoryTest {

    private StudySessionRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File tempFile = tempDir.resolve("test-sessions.json").toFile();
        repository = new InMemoryStudySessionRepository(tempFile.getAbsolutePath());
    }

    @Test
    @DisplayName("Deve salvar e recuperar uma sessão por ID")
    void testSaveAndFindById() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 15, 14, 0);
        LocalDateTime end = start.plusMinutes(90);

        StudySession session = new StudySession("subj-1", "Algoritmos", "Grafos",
                start, end, ActivityType.EXERCICIOS, SessionStatus.PLANEJADA, "Notas");

        StudySession saved = repository.save(session);
        assertNotNull(saved.getId());

        Optional<StudySession> retrieved = repository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Algoritmos", retrieved.get().getSubjectName());
        assertEquals("Grafos", retrieved.get().getTopic());
        assertEquals(90, retrieved.get().getDurationMinutes());
    }

    @Test
    @DisplayName("Deve remover uma sessão existente com sucesso")
    void testDeleteById() {
        LocalDateTime start = LocalDateTime.now();
        StudySession session = new StudySession("subj-2", "Cálculo", "Limites",
                start, start.plusMinutes(60), ActivityType.TEORIA, SessionStatus.PLANEJADA, "");

        repository.save(session);
        assertTrue(repository.findById(session.getId()).isPresent());

        boolean deleted = repository.deleteById(session.getId());
        assertTrue(deleted);
        assertFalse(repository.findById(session.getId()).isPresent());
    }

    @Test
    @DisplayName("Deve filtrar sessões por intervalo de datas")
    void testFindByDateRange() {
        LocalDate today = LocalDate.now();

        StudySession sToday = new StudySession("subj-1", "Algoritmos", "Arrays",
                today.atTime(10, 0), today.atTime(11, 0), ActivityType.TEORIA, SessionStatus.CONCLUIDA, "");
        StudySession sPast = new StudySession("subj-2", "Cálculo", "Integrais",
                today.minusDays(20).atTime(10, 0), today.minusDays(20).atTime(11, 0), ActivityType.EXERCICIOS, SessionStatus.CONCLUIDA, "");

        repository.save(sToday);
        repository.save(sPast);

        List<StudySession> inRange = repository.findByDateRange(today.minusDays(5), today.plusDays(1));
        assertTrue(inRange.contains(sToday));
        assertFalse(inRange.contains(sPast));
    }
}
