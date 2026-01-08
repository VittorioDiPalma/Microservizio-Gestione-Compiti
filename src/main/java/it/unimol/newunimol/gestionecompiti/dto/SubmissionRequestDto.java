package it.unimol.newunimol.gestionecompiti.dto;

public record SubmissionRequestDto(
    String assignmentId,
    String studentId,
    String filePath
) {}
