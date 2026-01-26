package it.unimol.newunimol.gestionecompiti.client.dto;

import java.time.LocalDateTime;

public class CourseEnrollmentDto {
    private String id;
    private String courseId;
    private String studentId;
    private String teacherId;
    private EnrollmentStatus status;
    private LocalDateTime enrollmentDate;
    private LocalDateTime approvedDate;
    private String notes;

    public CourseEnrollmentDto() {
    }

    public CourseEnrollmentDto(String id, String courseId, String studentId, String teacherId,
            EnrollmentStatus status,
            LocalDateTime enrollmentDate, LocalDateTime approvedDate, String notes) {
        this.id = id;
        this.courseId = courseId;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.status = status;
        this.enrollmentDate = enrollmentDate;
        this.approvedDate = approvedDate;
        this.notes = notes;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
