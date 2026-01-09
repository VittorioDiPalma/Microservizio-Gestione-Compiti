package it.unimol.newunimol.gestionecompiti.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String assignmentId;
    
    @Column(nullable = false)
    private String studentId;
    
    @Column(length = 5000)
    private String content;
    
    @ElementCollection
    @CollectionTable(name = "submission_attachments", joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "attachment_url")
    private List<String> attachments = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;
    
    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}