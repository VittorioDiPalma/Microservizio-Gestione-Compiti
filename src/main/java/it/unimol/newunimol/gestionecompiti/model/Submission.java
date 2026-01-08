package it.unimol.newunimol.gestionecompiti.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @NotNull
    @Column(name = "assignment_id", nullable = false)
    private String assignmentId;

    @NotNull
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @NotNull
    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    @Column(name = "file_path")
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    @PrePersist
    public void ensureId(){
        if (this.id == null || this.id.isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}