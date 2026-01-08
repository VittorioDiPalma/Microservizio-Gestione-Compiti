package it.unimol.newunimol.gestionecompiti.dto;

import java.time.LocalDate;

public record AssignmentRequestDto(
    String title,
    String description,
    LocalDate dueDate,
    Long courseId,
    String professorId,
    String attachmentPath
) {}
