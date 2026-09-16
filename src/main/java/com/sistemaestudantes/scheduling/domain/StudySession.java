package com.sistemaestudantes.scheduling.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade central representando um bloco/sessão de estudo agendado ou realizado.
 */
public class StudySession {

    private String id;
    private String subjectId;
    private String subjectName;
    private String topic;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private long durationMinutes;
    private ActivityType activityType;
    private SessionStatus status;
    private SyncStatus syncStatus;
    private String externalEventId;
    private String syncErrorMessage;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public StudySession() {
        // Construtor padrão para deserialização JSON
        this.id = UUID.randomUUID().toString();
        this.status = SessionStatus.PLANEJADA;
        this.syncStatus = SyncStatus.NAO_SINCRONIZADO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public StudySession(String subjectId, String subjectName, String topic,
                        LocalDateTime startTime, LocalDateTime endTime,
                        ActivityType activityType, SessionStatus status, String notes) {
        this.id = UUID.randomUUID().toString();
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topic = topic;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = calculateDuration(startTime, endTime);
        this.activityType = activityType != null ? activityType : ActivityType.TEORIA;
        this.status = status != null ? status : SessionStatus.PLANEJADA;
        this.syncStatus = SyncStatus.NAO_SINCRONIZADO;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        long minutes = Duration.between(start, end).toMinutes();
        return Math.max(0, minutes);
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        this.durationMinutes = calculateDuration(this.startTime, this.endTime);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.durationMinutes = calculateDuration(this.startTime, this.endTime);
        this.updatedAt = LocalDateTime.now();
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
        this.updatedAt = LocalDateTime.now();
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public void setExternalEventId(String externalEventId) {
        this.externalEventId = externalEventId;
    }

    public String getSyncErrorMessage() {
        return syncErrorMessage;
    }

    public void setSyncErrorMessage(String syncErrorMessage) {
        this.syncErrorMessage = syncErrorMessage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudySession that = (StudySession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
