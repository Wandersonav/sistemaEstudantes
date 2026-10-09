package com.sistemaestudantes.scheduling.repository;

import com.sistemaestudantes.database.DatabaseConfig;
import com.sistemaestudantes.scheduling.domain.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repositório relacional de matérias / disciplinas do sistema com suporte a PostgreSQL
 * e resiliência com fallback em memória.
 */
public class SubjectRepository {

    private final DatabaseConfig databaseConfig;
    private final List<Subject> fallbackSubjects = new CopyOnWriteArrayList<>();

    public SubjectRepository() {
        this(null);
    }

    public SubjectRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        seedDefaultSubjects();
        if (databaseConfig != null && databaseConfig.isConnected()) {
            syncDefaultSubjectsToDb();
        }
    }

    private void seedDefaultSubjects() {
        fallbackSubjects.add(new Subject("subj-1", "Algoritmos e Estruturas de Dados", "AED", "#3A7D8C")); // 1. Azul-petróleo
        fallbackSubjects.add(new Subject("subj-2", "Cálculo Diferencial e Integral", "CALC", "#7FB3D1"));   // 2. Azul-lagoa
        fallbackSubjects.add(new Subject("subj-3", "Arquitetura de Software", "ARQ", "#7FA99B"));          // 3. Verde-sálvia
        fallbackSubjects.add(new Subject("subj-4", "Banco de Dados e SQL", "BD", "#8A9BAA"));              // 4. Cinza-azulado
        fallbackSubjects.add(new Subject("subj-5", "Redes de Computadores", "REDES", "#5F7F99"));          // 5. Azul-aço
        fallbackSubjects.add(new Subject("subj-6", "Inteligência Artificial e ML", "IA", "#C9B99A"));       // 6. Areia suave
    }

    private void syncDefaultSubjectsToDb() {
        for (Subject s : fallbackSubjects) {
            saveToDb(s);
        }
    }

    public List<Subject> findAll() {
        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return Collections.unmodifiableList(new ArrayList<>(fallbackSubjects));
        }

        String sql = "SELECT id, name, code, hex_color FROM subjects ORDER BY id ASC";
        List<Subject> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Subject(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getString("hex_color")
                ));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao listar disciplinas do banco: " + e.getMessage());
        }
        return Collections.unmodifiableList(new ArrayList<>(fallbackSubjects));
    }

    public Optional<Subject> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();

        if (databaseConfig != null && databaseConfig.isConnected()) {
            String sql = "SELECT id, name, code, hex_color FROM subjects WHERE id = ?";
            try (Connection conn = databaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Subject(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("code"),
                                rs.getString("hex_color")
                        ));
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ [PostgreSQL] Erro ao buscar disciplina por ID: " + e.getMessage());
            }
        }
        return fallbackSubjects.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst();
    }

    public Optional<Subject> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        if (databaseConfig != null && databaseConfig.isConnected()) {
            String sql = "SELECT id, name, code, hex_color FROM subjects WHERE LOWER(name) = LOWER(?)";
            try (Connection conn = databaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Subject(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("code"),
                                rs.getString("hex_color")
                        ));
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ [PostgreSQL] Erro ao buscar disciplina por nome: " + e.getMessage());
            }
        }
        return fallbackSubjects.stream().filter(s -> s.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    public Subject save(Subject subject) {
        if (subject == null) throw new IllegalArgumentException("A disciplina não pode ser nula.");

        findById(subject.getId()).ifPresent(fallbackSubjects::remove);
        fallbackSubjects.add(subject);

        if (databaseConfig != null && databaseConfig.isConnected()) {
            saveToDb(subject);
        }
        return subject;
    }

    private void saveToDb(Subject subject) {
        String sql = """
            INSERT INTO subjects (id, name, code, hex_color)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                code = EXCLUDED.code,
                hex_color = EXCLUDED.hex_color
        """;
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getId());
            stmt.setString(2, subject.getName());
            stmt.setString(3, subject.getCode());
            stmt.setString(4, subject.getHexColor());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao salvar disciplina no banco: " + e.getMessage());
        }
    }
}
