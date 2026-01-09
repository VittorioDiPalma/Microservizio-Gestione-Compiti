package it.unimol.newunimol.gestionecompiti.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponseDto(
    String id,
    String title,
    String description,
    LocalDateTime dueDate,
    String courseId,
    String teacherId,
    List<String> attachments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int totalSubmissions
) {}