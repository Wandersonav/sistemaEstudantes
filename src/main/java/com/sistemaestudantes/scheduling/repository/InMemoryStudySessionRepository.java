package com.sistemaestudantes.scheduling.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.domain.SyncStatus;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementação em memória e persistente em arquivo JSON do repositório de sessões de estudo.
 */
public class InMemoryStudySessionRepository implements StudySessionRepository {

    private final Map<String, StudySession> storage = new ConcurrentHashMap<>();
    private final File storageFile;
    private final ObjectMapper objectMapper;

    public InMemoryStudySessionRepository() {
        this("data/sessions.json");
    }

    public InMemoryStudySessionRepository(String filePath) {
        this.storageFile = new File(filePath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        loadFromFile();
        if (storage.isEmpty()) {
            seedSampleData();
            saveToFile();
        }
    }

    @Override
    public synchronized StudySession save(StudySession session) {
        if (session == null) throw new IllegalArgumentException("A sessão não pode ser nula.");
        session.setUpdatedAt(LocalDateTime.now());
        storage.put(session.getId(), session);
        saveToFile();
        return session;
    }

    @Override
    public Optional<StudySession> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<StudySession> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(StudySession::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<StudySession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return storage.values().stream()
                .filter(s -> s.getStartTime() != null &&
                        !s.getStartTime().isBefore(start) &&
                        !s.getStartTime().isAfter(end))
                .sorted(Comparator.comparing(StudySession::getStartTime))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean deleteById(String id) {
        boolean removed = storage.remove(id) != null;
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    @Override
    public int count() {
        return storage.size();
    }

    private void loadFromFile() {
        if (storageFile.exists() && storageFile.length() > 0) {
            try {
                List<StudySession> loaded = objectMapper.readValue(storageFile, new TypeReference<List<StudySession>>() {});
                for (StudySession s : loaded) {
                    storage.put(s.getId(), s);
                }
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao carregar sessões salvas do arquivo: " + e.getMessage());
            }
        }
    }

    private synchronized void saveToFile() {
        try {
            File parentDir = storageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(storageFile, new ArrayList<>(storage.values()));
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível salvar as sessões em arquivo: " + e.getMessage());
        }
    }

    /**
     * Alimenta a base com sessões realistas distribuídas nos últimos dias para popular métricas e heatmap.
     */
    private void seedSampleData() {
        LocalDate today = LocalDate.now();

        // Sessões de exemplo nos últimos dias
        addSample(today.minusDays(6), 14, 0, 90, "subj-1", "Algoritmos e Estruturas de Dados", "Árvores Binárias e AVL", ActivityType.TEORIA, SessionStatus.CONCLUIDA);
        addSample(today.minusDays(5), 10, 30, 60, "subj-2", "Cálculo Diferencial e Integral", "Derivadas Parciais", ActivityType.EXERCICIOS, SessionStatus.CONCLUIDA);
        addSample(today.minusDays(4), 16, 0, 120, "subj-4", "Banco de Dados e SQL", "Modelagem ER e Normalização", ActivityType.TEORIA, SessionStatus.CONCLUIDA);
        addSample(today.minusDays(3), 9, 0, 80, "subj-3", "Arquitetura de Software", "Microsserviços e Event-Driven", ActivityType.REVISAO, SessionStatus.CONCLUIDA);
        addSample(today.minusDays(2), 15, 0, 100, "subj-1", "Algoritmos e Estruturas de Dados", "Grafos (Dijkstra e BFS)", ActivityType.EXERCICIOS, SessionStatus.CONCLUIDA);
        addSample(today.minusDays(1), 11, 0, 75, "subj-5", "Redes de Computadores", "Protocolo TCP/IP e Camada de Transporte", ActivityType.REVISAO, SessionStatus.CONCLUIDA);

        // Sessão de hoje concluída
        addSample(today, 9, 0, 90, "subj-6", "Inteligência Artificial e ML", "Regressão Linear e Gradiente Descendente", ActivityType.TEORIA, SessionStatus.CONCLUIDA);

        // Sessão planejada para hoje mais tarde
        StudySession planned = new StudySession(
                "subj-1", "Algoritmos e Estruturas de Dados", "Programação Dinâmica",
                today.atTime(15, 0), today.atTime(16, 30),
                ActivityType.EXERCICIOS, SessionStatus.PLANEJADA,
                "Resolver 5 exercícios da lista LeetCode."
        );
        planned.setSyncStatus(SyncStatus.SINCRONIZADO);
        planned.setExternalEventId("gcal-evt-seed-12345");
        storage.put(planned.getId(), planned);
    }

    private void addSample(LocalDate date, int hour, int minute, int durationMin,
                           String subjId, String subjName, String topic,
                           ActivityType type, SessionStatus status) {
        LocalDateTime start = date.atTime(hour, minute);
        LocalDateTime end = start.plusMinutes(durationMin);
        StudySession s = new StudySession(subjId, subjName, topic, start, end, type, status, "Sessão de estudo realizada.");
        s.setSyncStatus(SyncStatus.SINCRONIZADO);
        s.setExternalEventId("gcal-" + s.getId().substring(0, 8));
        storage.put(s.getId(), s);
    }
}
