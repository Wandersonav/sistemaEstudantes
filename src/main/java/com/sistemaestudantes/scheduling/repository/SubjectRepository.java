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
        subjects.add(new Subject("subj-1", "Algoritmos e Estruturas de Dados", "AED", "#3A7D8C")); // 1. Azul-petróleo
        subjects.add(new Subject("subj-2", "Cálculo Diferencial e Integral", "CALC", "#7FB3D1"));   // 2. Azul-lagoa
        subjects.add(new Subject("subj-3", "Arquitetura de Software", "ARQ", "#7FA99B"));          // 3. Verde-sálvia
        subjects.add(new Subject("subj-4", "Banco de Dados e SQL", "BD", "#8A9BAA"));              // 4. Cinza-azulado
        subjects.add(new Subject("subj-5", "Redes de Computadores", "REDES", "#5F7F99"));          // 5. Azul-aço
        subjects.add(new Subject("subj-6", "Inteligência Artificial e ML", "IA", "#C9B99A"));       // 6. Areia suave
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
