package com.sistemaestudantes.scheduling;

import com.sistemaestudantes.database.DatabaseConfig;
import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.repository.PostgresStudySessionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes automatizados para o repositório relacional PostgreSQL com fallback em memória.
 */
class PostgresStudySessionRepositoryTest {

    private static DatabaseConfig dbConfig;
    private static PostgresStudySessionRepository repository;

    @BeforeAll
    static void setUp() {
        dbConfig = new DatabaseConfig();
        dbConfig.init();
        repository = new PostgresStudySessionRepository(dbConfig);
    }

    @AfterAll
    static void tearDown() {
        if (dbConfig != null) {
            dbConfig.close();
        }
    }

    @Test
    @DisplayName("Deve salvar e recuperar sessão via repositório PostgreSQL")
    void testSaveAndFindById() {
        LocalDateTime start = LocalDateTime.now().withNano(0);
        LocalDateTime end = start.plusMinutes(60);

        StudySession session = new StudySession(
                "subj-1",
                "Algoritmos e Estruturas de Dados",
                "Grafos e Busca em Profundidade",
                start,
                end,
                ActivityType.EXERCICIOS,
                SessionStatus.PLANEJADA,
                "Teste relacional PostgreSQL"
        );

        StudySession saved = repository.save(session);
        assertNotNull(saved.getId());

        Optional<StudySession> found = repository.findById(saved.getId());
        assertTrue(found.isPresent(), "A sessão salva deve ser encontrada");
        assertEquals("Algoritmos e Estruturas de Dados", found.get().getSubjectName());
        assertEquals("Grafos e Busca em Profundidade", found.get().getTopic());
        assertEquals(60, found.get().getDurationMinutes());

        // Limpeza
        repository.deleteById(saved.getId());
    }

    @Test
    @DisplayName("Deve atualizar status e tópico de uma sessão existente")
    void testUpdateSession() {
        LocalDateTime start = LocalDateTime.now().withNano(0);
        StudySession session = new StudySession(
                "subj-2",
                "Cálculo Diferencial e Integral",
                "Integrais Triplas",
                start,
                start.plusMinutes(90),
                ActivityType.TEORIA,
                SessionStatus.PLANEJADA,
                "Inicial"
        );

        repository.save(session);

        session.setStatus(SessionStatus.CONCLUIDA);
        session.setTopic("Integrais Triplas - Aplicações Físicas");
        session.setNotes("Concluído com êxito");
        repository.save(session);

        Optional<StudySession> updated = repository.findById(session.getId());
        assertTrue(updated.isPresent());
        assertEquals(SessionStatus.CONCLUIDA, updated.get().getStatus());
        assertEquals("Integrais Triplas - Aplicações Físicas", updated.get().getTopic());
        assertEquals("Concluído com êxito", updated.get().getNotes());

        // Limpeza
        repository.deleteById(session.getId());
    }

    @Test
    @DisplayName("Deve listar todas as sessões e filtrar por intervalo de datas")
    void testFindAllAndFindByDateRange() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atTime(10, 0);

        StudySession session = new StudySession(
                "subj-3",
                "Banco de Dados e SQL",
                "Modelagem Relacional e FKs",
                start,
                start.plusMinutes(45),
                ActivityType.EXERCICIOS,
                SessionStatus.EM_ANDAMENTO,
                "Teste filtro relacional"
        );

        repository.save(session);

        List<StudySession> all = repository.findAll();
        assertFalse(all.isEmpty(), "A lista de sessões não pode estar vazia");

        List<StudySession> rangeSessions = repository.findByDateRange(today, today.plusDays(1));
        assertFalse(rangeSessions.isEmpty(), "Deve encontrar sessões no intervalo de hoje");

        // Limpeza
        repository.deleteById(session.getId());
    }
}
