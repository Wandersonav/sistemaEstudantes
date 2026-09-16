package com.sistemaestudantes.scheduling.repository;

import com.sistemaestudantes.scheduling.domain.StudySession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositório para acesso e persistência de sessões de estudo.
 */
public interface StudySessionRepository {

    StudySession save(StudySession session);

    Optional<StudySession> findById(String id);

    List<StudySession> findAll();

    List<StudySession> findByDateRange(LocalDate startDate, LocalDate endDate);

    boolean deleteById(String id);

    int count();
}
