package it.unimol.newunimol.gestionecompiti.messaging.event;

import java.io.Serializable;

/**
 * Evento ricevuto quando un corso viene eliminato in Gestione-Corsi
 * Elimina tutti gli assignment (e relative submission) del corso
 */
public class CourseDeletedEvent implements Serializable {
    
    private String eventType;
    private String courseId;
    private String courseName;
    private String deletedBy;
    private Long timestamp;
    
    public CourseDeletedEvent() {}
    
    public CourseDeletedEvent(String eventType, String courseId, String courseName, String deletedBy, Long timestamp) {
        this.eventType = eventType;
        this.courseId = courseId;
        this.courseName = courseName;
        this.deletedBy = deletedBy;
        this.timestamp = timestamp;
    }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "CourseDeletedEvent{" +
                "eventType='" + eventType + '\'' +
                ", courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", deletedBy='" + deletedBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
