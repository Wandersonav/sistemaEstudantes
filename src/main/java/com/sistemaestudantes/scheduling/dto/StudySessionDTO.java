package com.sistemaestudantes.scheduling.dto;

import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) para desacoplamento de entradas de dados e requisições de sessão.
 */
public class StudySessionDTO {

    private String id;
    private String subjectId;
    private String subjectName;
    private String topic;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ActivityType activityType;
    private SessionStatus status;
    private String notes;
    private boolean syncWithGoogleCalendar;

    public StudySessionDTO() {
    }

    public StudySessionDTO(String subjectId, String subjectName, String topic,
                           LocalDateTime startTime, LocalDateTime endTime,
                           ActivityType activityType, SessionStatus status,
                           String notes, boolean syncWithGoogleCalendar) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topic = topic;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activityType = activityType;
        this.status = status;
        this.notes = notes;
        this.syncWithGoogleCalendar = syncWithGoogleCalendar;
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
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isSyncWithGoogleCalendar() {
        return syncWithGoogleCalendar;
    }

    public void setSyncWithGoogleCalendar(boolean syncWithGoogleCalendar) {
        this.syncWithGoogleCalendar = syncWithGoogleCalendar;
    }
}
