package it.unimol.newunimol.gestionecompiti.dto;

import java.time.LocalDateTime;

public record AssignmentResponseDto(
        String id,
        String title,
        String description,
        LocalDateTime dueDate,
        String courseId,
        String teacherId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int totalSubmissions) {
}