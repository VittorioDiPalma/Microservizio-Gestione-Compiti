package it.unimol.newunimol.gestionecompiti.model;

import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;


@Entity
@Table(name = "assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDate creationDate;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "attachment_path")
    private String attachmentPath;

    @NotNull
    @Column(name = "course_id", nullable = false)
    private String courseId;

    @NotNull
    @Column(name = "professor_id", nullable = false)
    private String professorId;

    @PrePersist
    public void ensureId(){
        if (this.id == null || this.id.isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
