package it.unimol.newunimol.gestionecompiti.dto;

import java.time.LocalDate;

public record AssignmentResponseDto(
    long id,
    String title,
    String description,
    LocalDate creationDate,
    LocalDate dueDate,
    String attachmentPath,
    Long courseId,
    String professorId
) {}
