package com.sistemaestudantes.scheduling.repository;

import com.sistemaestudantes.database.DatabaseConfig;
import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.domain.SyncStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação relacional robusta do repositório de sessões de estudo utilizando PostgreSQL.
 * Fornece integridade referencial com a tabela 'disciplinas', índices otimizados
 * e fallback automático em memória para ambientes de testes desacoplados.
 */
public class PostgresStudySessionRepository implements StudySessionRepository {

    private final DatabaseConfig databaseConfig;
    private final InMemoryStudySessionRepository fallbackRepo;

    public PostgresStudySessionRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.fallbackRepo = new InMemoryStudySessionRepository();
    }

    @Override
    public StudySession save(StudySession session) {
        if (session == null) throw new IllegalArgumentException("A sessão não pode ser nula.");
        session.setUpdatedAt(LocalDateTime.now());

        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackRepo.save(session);
        }

        // Garante integridade referencial da disciplina antes de inserir a sessão
        ensureSubjectExists(session.getSubjectId(), session.getSubjectName());

        String sql = """
            INSERT INTO sessoes_estudo (
                id, disciplina_id, disciplina_nome, topico, data_inicio, data_fim,
                duracao_minutos, tipo_atividade, status, status_sincronizacao,
                evento_externo_id, mensagem_erro_sincronizacao, observacoes, criado_em, atualizado_em
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                disciplina_id = EXCLUDED.disciplina_id,
                disciplina_nome = EXCLUDED.disciplina_nome,
                topico = EXCLUDED.topico,
                data_inicio = EXCLUDED.data_inicio,
                data_fim = EXCLUDED.data_fim,
                duracao_minutos = EXCLUDED.duracao_minutos,
                tipo_atividade = EXCLUDED.tipo_atividade,
                status = EXCLUDED.status,
                status_sincronizacao = EXCLUDED.status_sincronizacao,
                evento_externo_id = EXCLUDED.evento_externo_id,
                mensagem_erro_sincronizacao = EXCLUDED.mensagem_erro_sincronizacao,
                observacoes = EXCLUDED.observacoes,
                atualizado_em = CURRENT_TIMESTAMP
        """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getId());
            stmt.setString(2, session.getSubjectId() != null ? session.getSubjectId() : "subj-1");
            stmt.setString(3, session.getSubjectName() != null ? session.getSubjectName() : "Geral");
            stmt.setString(4, session.getTopic() != null ? session.getTopic() : "Sessão de Estudo");

            LocalDateTime start = session.getStartTime() != null ? session.getStartTime() : LocalDateTime.now();
            LocalDateTime end = session.getEndTime() != null ? session.getEndTime() : start.plusMinutes(session.getDurationMinutes() > 0 ? session.getDurationMinutes() : 25);
            stmt.setTimestamp(5, Timestamp.valueOf(start));
            stmt.setTimestamp(6, Timestamp.valueOf(end));
            stmt.setInt(7, (int) (session.getDurationMinutes() > 0 ? session.getDurationMinutes() : 25));

            stmt.setString(8, session.getActivityType() != null ? session.getActivityType().name() : ActivityType.TEORIA.name());
            stmt.setString(9, session.getStatus() != null ? session.getStatus().name() : SessionStatus.PLANEJADA.name());
            stmt.setString(10, session.getSyncStatus() != null ? session.getSyncStatus().name() : SyncStatus.NAO_SINCRONIZADO.name());
            stmt.setString(11, session.getExternalEventId());
            stmt.setString(12, session.getSyncErrorMessage());
            stmt.setString(13, session.getNotes());

            LocalDateTime created = session.getCreatedAt() != null ? session.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(14, Timestamp.valueOf(created));
            stmt.setTimestamp(15, Timestamp.valueOf(session.getUpdatedAt()));

            stmt.executeUpdate();
            fallbackRepo.save(session);
            return session;
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao salvar sessão no banco de dados, usando fallback local: " + e.getMessage());
            return fallbackRepo.save(session);
        }
    }

    @Override
    public Optional<StudySession> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();

        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackRepo.findById(id);
        }

        String sql = "SELECT * FROM sessoes_estudo WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao buscar sessão por ID: " + e.getMessage());
        }
        return fallbackRepo.findById(id);
    }

    @Override
    public List<StudySession> findAll() {
        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackRepo.findAll();
        }

        String sql = "SELECT * FROM sessoes_estudo ORDER BY data_inicio DESC";
        List<StudySession> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToSession(rs));
            }
            return list;
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao listar sessões: " + e.getMessage());
            return fallbackRepo.findAll();
        }
    }

    @Override
    public List<StudySession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackRepo.findByDateRange(startDate, endDate);
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        String sql = "SELECT * FROM sessoes_estudo WHERE data_inicio >= ? AND data_inicio <= ? ORDER BY data_inicio ASC";
        List<StudySession> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao filtrar sessões por data: " + e.getMessage());
            return fallbackRepo.findByDateRange(startDate, endDate);
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) return false;

        boolean fallbackDeleted = fallbackRepo.deleteById(id);

        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackDeleted;
        }

        String sql = "DELETE FROM sessoes_estudo WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0 || fallbackDeleted;
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao deletar sessão: " + e.getMessage());
            return fallbackDeleted;
        }
    }

    @Override
    public int count() {
        if (databaseConfig == null || !databaseConfig.isConnected()) {
            return fallbackRepo.count();
        }

        String sql = "SELECT COUNT(*) FROM sessoes_estudo";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao contar sessões: " + e.getMessage());
        }
        return fallbackRepo.count();
    }

    private void ensureSubjectExists(String subjectId, String subjectName) {
        if (subjectId == null || subjectId.isBlank()) return;
        String sql = "INSERT INTO disciplinas (id, nome, codigo, cor_hex) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subjectId);
            stmt.setString(2, subjectName != null && !subjectName.isBlank() ? subjectName : "Geral");
            stmt.setString(3, subjectId.toUpperCase().replace("SUBJ-", "S"));
            stmt.setString(4, "#3A7D8C");
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private StudySession mapResultSetToSession(ResultSet rs) throws SQLException {
        StudySession s = new StudySession();
        s.setId(rs.getString("id"));
        s.setSubjectId(rs.getString("disciplina_id"));
        s.setSubjectName(rs.getString("disciplina_nome"));
        s.setTopic(rs.getString("topico"));

        Timestamp startTs = rs.getTimestamp("data_inicio");
        if (startTs != null) s.setStartTime(startTs.toLocalDateTime());

        Timestamp endTs = rs.getTimestamp("data_fim");
        if (endTs != null) s.setEndTime(endTs.toLocalDateTime());

        s.setDurationMinutes(rs.getInt("duracao_minutos"));

        String actStr = rs.getString("tipo_atividade");
        if (actStr != null) {
            try { s.setActivityType(ActivityType.valueOf(actStr)); } catch (Exception ignored) {}
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try { s.setStatus(SessionStatus.valueOf(statusStr)); } catch (Exception ignored) {}
        }

        String syncStr = rs.getString("status_sincronizacao");
        if (syncStr != null) {
            try { s.setSyncStatus(SyncStatus.valueOf(syncStr)); } catch (Exception ignored) {}
        }

        s.setExternalEventId(rs.getString("evento_externo_id"));
        s.setSyncErrorMessage(rs.getString("mensagem_erro_sincronizacao"));
        s.setNotes(rs.getString("observacoes"));

        Timestamp createdTs = rs.getTimestamp("criado_em");
        if (createdTs != null) s.setCreatedAt(createdTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("atualizado_em");
        if (updatedTs != null) s.setUpdatedAt(updatedTs.toLocalDateTime());

        return s;
    }
}
