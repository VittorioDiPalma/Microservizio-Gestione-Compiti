package it.unimol.newunimol.gestionecompiti.messaging.event;

import java.io.Serializable;

/**
 * Evento ricevuto quando uno studente si disiscrive da un corso in
 * Gestione-Iscrizioni
 * Elimina tutte le consegne dello studente per quel corso (e i relativi file)
 */
public class StudentUnenrolledEvent implements Serializable {

    private String eventType;
    private String enrollmentId;
    private String studentId;
    private String courseId;
    private String unenrolledBy;
    private Long timestamp;

    public StudentUnenrolledEvent() {
    }

    public StudentUnenrolledEvent(String eventType, String enrollmentId, String studentId,
            String courseId, String unenrolledBy, Long timestamp) {
        this.eventType = eventType;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.unenrolledBy = unenrolledBy;
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getUnenrolledBy() {
        return unenrolledBy;
    }

    public void setUnenrolledBy(String unenrolledBy) {
        this.unenrolledBy = unenrolledBy;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StudentUnenrolledEvent{" +
                "eventType='" + eventType + '\'' +
                ", enrollmentId='" + enrollmentId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", courseId='" + courseId + '\'' +
                ", unenrolledBy='" + unenrolledBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
