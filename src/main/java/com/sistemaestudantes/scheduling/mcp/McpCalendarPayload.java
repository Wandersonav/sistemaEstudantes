package com.sistemaestudantes.scheduling.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sistemaestudantes.scheduling.domain.StudySession;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Estrutura de payload padronizada para ferramentas do Google Calendar via MCP (Model Context Protocol).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpCalendarPayload {

    private final String summary;
    private final String startDateTime;
    private final String endDateTime;
    private final String description;
    private final Map<String, Object> metadata;

    public McpCalendarPayload(String summary, String startDateTime, String endDateTime,
                              String description, Map<String, Object> metadata) {
        this.summary = summary;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.description = description;
        this.metadata = metadata;
    }

    /**
     * Constrói o payload estrito a partir de uma sessão de estudo, seguindo o padrão:
     * Título: "[Estudo] {Matéria} - {Tópico}"
     * Corpo: Metadados estruturados de sessão e tipo de atividade.
     */
    public static McpCalendarPayload fromStudySession(StudySession session) {
        String topicSuffix = (session.getTopic() != null && !session.getTopic().isBlank())
                ? " - " + session.getTopic().trim()
                : "";
        String summary = String.format("[Estudo] %s%s", session.getSubjectName(), topicSuffix);

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String startStr = session.getStartTime() != null ? session.getStartTime().format(isoFormatter) : null;
        String endStr = session.getEndTime() != null ? session.getEndTime().format(isoFormatter) : null;

        Map<String, Object> meta = new HashMap<>();
        meta.put("origem", "sistemaEstudantes");
        meta.put("sessionId", session.getId());
        meta.put("subjectId", session.getSubjectId());
        meta.put("activityType", session.getActivityType() != null ? session.getActivityType().name() : "TEORIA");
        meta.put("status", session.getStatus() != null ? session.getStatus().name() : "PLANEJADA");
        meta.put("durationMinutes", session.getDurationMinutes());

        StringBuilder descBuilder = new StringBuilder();
        descBuilder.append("📘 Sessão de Estudos - Sistema de Estudantes\n");
        descBuilder.append("----------------------------------------\n");
        descBuilder.append("Disciplina: ").append(session.getSubjectName()).append("\n");
        if (session.getTopic() != null && !session.getTopic().isBlank()) {
            descBuilder.append("Tópico: ").append(session.getTopic()).append("\n");
        }
        descBuilder.append("Atividade: ").append(session.getActivityType().getLabel()).append("\n");
        descBuilder.append("Status: ").append(session.getStatus().getLabel()).append("\n");
        descBuilder.append("Duração estimada: ").append(session.getDurationMinutes()).append(" minutos\n");
        if (session.getNotes() != null && !session.getNotes().isBlank()) {
            descBuilder.append("\nObservações:\n").append(session.getNotes()).append("\n");
        }
        descBuilder.append("\nID da Sessão: ").append(session.getId());

        return new McpCalendarPayload(summary, startStr, endStr, descBuilder.toString(), meta);
    }

    public String getSummary() {
        return summary;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
