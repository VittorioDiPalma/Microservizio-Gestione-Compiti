package it.unimol.newunimol.gestionecompiti.dto;

import java.time.LocalDate;

public record AssignmentResponseDto(
    String id,
    String title,
    String description,
    LocalDate creationDate,
    LocalDate dueDate,
    String attachmentPath,
    String courseId,
    String professorId
) {}
