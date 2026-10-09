package com.sistemaestudantes.scheduling;

import com.sistemaestudantes.database.DatabaseConfig;
import com.sistemaestudantes.scheduling.domain.Subject;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes automatizados para o repositório de disciplinas com suporte a PostgreSQL
 * e mapeamento de colunas em português.
 */
class SubjectRepositoryTest {

    private static DatabaseConfig dbConfig;
    private static SubjectRepository repository;

    @BeforeAll
    static void setUp() {
        dbConfig = new DatabaseConfig();
        dbConfig.init();
        repository = new SubjectRepository(dbConfig);
    }

    @AfterAll
    static void tearDown() {
        if (dbConfig != null) {
            dbConfig.close();
        }
    }

    @Test
    @DisplayName("Deve listar todas as disciplinas cadastradas")
    void testFindAll() {
        List<Subject> subjects = repository.findAll();
        assertNotNull(subjects);
        assertFalse(subjects.isEmpty(), "Deve conter disciplinas semeadas");
        assertTrue(subjects.stream().anyMatch(s -> s.getName().equals("Algoritmos e Estruturas de Dados")));
    }

    @Test
    @DisplayName("Deve buscar disciplina por ID")
    void testFindById() {
        Optional<Subject> subject = repository.findById("subj-1");
        assertTrue(subject.isPresent(), "Disciplina 'subj-1' deve existir");
        assertEquals("Algoritmos e Estruturas de Dados", subject.get().getName());
        assertEquals("AED", subject.get().getCode());
        assertEquals("#3A7D8C", subject.get().getHexColor());
    }

    @Test
    @DisplayName("Deve buscar disciplina por Nome")
    void testFindByName() {
        Optional<Subject> subject = repository.findByName("Cálculo Diferencial e Integral");
        assertTrue(subject.isPresent());
        assertEquals("CALC", subject.get().getCode());
    }

    @Test
    @DisplayName("Deve salvar e recuperar nova disciplina")
    void testSaveCustomSubject() {
        Subject custom = new Subject("subj-test-pt", "Engenharia de Requisitos", "REQ", "#1E88E5");
        Subject saved = repository.save(custom);
        assertNotNull(saved);

        Optional<Subject> retrieved = repository.findById("subj-test-pt");
        assertTrue(retrieved.isPresent());
        assertEquals("Engenharia de Requisitos", retrieved.get().getName());
        assertEquals("REQ", retrieved.get().getCode());
        assertEquals("#1E88E5", retrieved.get().getHexColor());
    }
}
