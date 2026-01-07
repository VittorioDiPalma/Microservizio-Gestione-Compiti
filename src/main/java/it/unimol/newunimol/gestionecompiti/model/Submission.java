package it.unimol.newunimol.gestionecompiti.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @NotBlank
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

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "feedback", length = 2000)
    private String feedback;
}