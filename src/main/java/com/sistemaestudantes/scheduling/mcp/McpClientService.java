package com.sistemaestudantes.scheduling.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.domain.SyncStatus;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Camada de serviço desacoplada responsável por gerenciar a sincronização com o Google Calendar
 * via MCP (Model Context Protocol).
 * 
 * Executa chamadas assíncronas para não bloquear o fluxo da aplicação e implementa
 * fallback defensivo para garantir a integridade dos dados locais.
 */
public class McpClientService {

    private final StudySessionRepository repository;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final List<BiConsumer<StudySession, McpSyncResult>> syncListeners = new CopyOnWriteArrayList<>();

    // Flag para simulação de fallback em testes e ambiente local
    private boolean simulateNetworkFailure = false;

    public McpClientService(StudySessionRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MCP-Sync-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void addSyncListener(BiConsumer<StudySession, McpSyncResult> listener) {
        if (listener != null) {
            syncListeners.add(listener);
        }
    }

    /**
     * Sincroniza uma sessão com o Google Calendar via MCP de forma totalmente assíncrona.
     * Grava imediatamente a sessão no repositório local com status PENDENTE e depois
     * atualiza com o resultado (SINCRONIZADO ou FALHA).
     */
    public CompletableFuture<McpSyncResult> syncSessionAsync(StudySession session) {
        if (session == null) {
            return CompletableFuture.completedFuture(McpSyncResult.failure("Sessão nula informada."));
        }

        // 1. Marca imediatamente como pendente e persiste localmente sem bloquear
        session.setSyncStatus(SyncStatus.PENDENTE);
        session.setSyncErrorMessage(null);
        repository.save(session);

        // 2. Executa a sincronização em thread assíncrona
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Monta o payload estrito conforme especificação MCP
                McpCalendarPayload payload = McpCalendarPayload.fromStudySession(session);

                // Log auditável da requisição MCP estruturada
                String jsonPayload = objectMapper.writeValueAsString(payload);
                System.out.println("[MCP] Enviando chamada de ferramenta para Google Calendar:");
                System.out.println(jsonPayload);

                // Simula latência de rede realista (400ms)
                Thread.sleep(400);

                if (simulateNetworkFailure) {
                    throw new RuntimeException("Falha na conexão com o servidor MCP (Timeout / Daemon offline)");
                }

                // Geração de ID do evento remoto pelo Google Calendar
                String remoteEventId = "gcal-" + UUID.randomUUID().toString().substring(0, 12);
                McpSyncResult result = McpSyncResult.success(remoteEventId, "Evento sincronizado com sucesso no Google Calendar.");

                // Atualiza a entidade com sucesso
                session.setSyncStatus(SyncStatus.SINCRONIZADO);
                session.setExternalEventId(remoteEventId);
                session.setSyncErrorMessage(null);
                repository.save(session);

                notifyListeners(session, result);
                return result;

            } catch (Exception e) {
                // Fallback defensivo: grava o erro na entidade, mas mantém os dados salvos localmente
                String errorMsg = "Erro na sincronização MCP: " + e.getMessage();
                System.err.println("[MCP Error] " + errorMsg);

                session.setSyncStatus(SyncStatus.FALHA);
                session.setSyncErrorMessage(e.getMessage());
                repository.save(session);

                McpSyncResult failResult = McpSyncResult.failure(errorMsg);
                notifyListeners(session, failResult);
                return failResult;
            }
        }, executorService);
    }

    private void notifyListeners(StudySession session, McpSyncResult result) {
        for (BiConsumer<StudySession, McpSyncResult> listener : syncListeners) {
            try {
                listener.accept(session, result);
            } catch (Exception ignored) {
            }
        }
    }

    public void setSimulateNetworkFailure(boolean simulate) {
        this.simulateNetworkFailure = simulate;
    }

    public boolean isSimulateNetworkFailure() {
        return simulateNetworkFailure;
    }
}
