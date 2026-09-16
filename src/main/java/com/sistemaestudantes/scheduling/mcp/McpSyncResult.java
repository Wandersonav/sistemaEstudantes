package com.sistemaestudantes.scheduling.mcp;

import java.time.LocalDateTime;

/**
 * Encapsula o resultado da operação assíncrona de sincronização via MCP.
 */
public class McpSyncResult {

    private final boolean success;
    private final String externalEventId;
    private final String message;
    private final LocalDateTime timestamp;

    private McpSyncResult(boolean success, String externalEventId, String message) {
        this.success = success;
        this.externalEventId = externalEventId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static McpSyncResult success(String eventId, String message) {
        return new McpSyncResult(true, eventId, message);
    }

    public static McpSyncResult failure(String errorMessage) {
        return new McpSyncResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
