package com.sistemaestudantes.scheduling.repository;

import com.sistemaestudantes.scheduling.domain.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repositório de matérias / disciplinas do sistema.
 */
public class SubjectRepository {

    private final List<Subject> subjects = new CopyOnWriteArrayList<>();

    public SubjectRepository() {
        seedDefaultSubjects();
    }

    private void seedDefaultSubjects() {
        subjects.add(new Subject("subj-1", "Algoritmos e Estruturas de Dados", "AED", "#3498DB"));
        subjects.add(new Subject("subj-2", "Cálculo Diferencial e Integral", "CALC", "#E74C3C"));
        subjects.add(new Subject("subj-3", "Arquitetura de Software", "ARQ", "#9B59B6"));
        subjects.add(new Subject("subj-4", "Banco de Dados e SQL", "BD", "#2ECC71"));
        subjects.add(new Subject("subj-5", "Redes de Computadores", "REDES", "#E67E22"));
        subjects.add(new Subject("subj-6", "Inteligência Artificial e ML", "IA", "#1ABC9C"));
    }

    public List<Subject> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(subjects));
    }

    public Optional<Subject> findById(String id) {
        return subjects.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst();
    }

    public Optional<Subject> findByName(String name) {
        return subjects.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    public Subject save(Subject subject) {
        if (subject == null) throw new IllegalArgumentException("A disciplina não pode ser nula.");
        findById(subject.getId()).ifPresent(subjects::remove);
        subjects.add(subject);
        return subject;
    }
}
