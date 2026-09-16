package com.sistemaestudantes.scheduling;

import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.domain.SyncStatus;
import com.sistemaestudantes.scheduling.mcp.McpCalendarPayload;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.mcp.McpSyncResult;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class McpClientServiceTest {

    private StudySessionRepository repository;
    private McpClientService mcpService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File tempFile = tempDir.resolve("test-mcp.json").toFile();
        repository = new InMemoryStudySessionRepository(tempFile.getAbsolutePath());
        mcpService = new McpClientService(repository);
    }

    @Test
    @DisplayName("Deve gerar o payload estruturado MCP com formato [Estudo] {Matéria} e metadados")
    void testMcpPayloadStructure() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 15, 14, 0);
        LocalDateTime end = start.plusMinutes(90);

        StudySession session = new StudySession(
                "subj-3", "Arquitetura de Software", "Padrões GoF",
                start, end, ActivityType.TEORIA, SessionStatus.PLANEJADA, "Focar no padrão Observer"
        );

        McpCalendarPayload payload = McpCalendarPayload.fromStudySession(session);

        assertEquals("[Estudo] Arquitetura de Software - Padrões GoF", payload.getSummary());
        assertTrue(payload.getDescription().contains("ID da Sessão: " + session.getId()));
        assertTrue(payload.getDescription().contains("Teoria e Leitura"));

        assertEquals("sistemaEstudantes", payload.getMetadata().get("origem"));
        assertEquals("TEORIA", payload.getMetadata().get("activityType"));
        assertEquals(90L, payload.getMetadata().get("durationMinutes"));
    }

    @Test
    @DisplayName("Deve sincronizar com sucesso assincronamente e atualizar status da sessão")
    void testSuccessfulAsyncSync() throws Exception {
        StudySession session = new StudySession(
                "subj-1", "Algoritmos", "Dijkstra",
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(60),
                ActivityType.EXERCICIOS, SessionStatus.PLANEJADA, ""
        );
        repository.save(session);

        CompletableFuture<McpSyncResult> future = mcpService.syncSessionAsync(session);
        McpSyncResult result = future.get();

        assertTrue(result.isSuccess());
        assertNotNull(result.getExternalEventId());
        assertTrue(result.getExternalEventId().startsWith("gcal-"));

        // Verificar entidade atualizada no repositório
        StudySession updated = repository.findById(session.getId()).orElseThrow();
        assertEquals(SyncStatus.SINCRONIZADO, updated.getSyncStatus());
        assertEquals(result.getExternalEventId(), updated.getExternalEventId());
    }

    @Test
    @DisplayName("Deve executar fallback defensivo em caso de falha de conexão sem perder a sessão interna")
    void testFallbackOnNetworkFailure() throws Exception {
        StudySession session = new StudySession(
                "subj-2", "Cálculo", "Séries de Taylor",
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(60),
                ActivityType.TEORIA, SessionStatus.PLANEJADA, ""
        );
        repository.save(session);

        // Ativa simulação de falha
        mcpService.setSimulateNetworkFailure(true);

        CompletableFuture<McpSyncResult> future = mcpService.syncSessionAsync(session);
        McpSyncResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("servidor MCP"));

        // A sessão local DEVE continuar preservada no repositório com status FALHA
        StudySession updated = repository.findById(session.getId()).orElseThrow();
        assertEquals(SyncStatus.FALHA, updated.getSyncStatus());
        assertNotNull(updated.getSyncErrorMessage());
    }
}
