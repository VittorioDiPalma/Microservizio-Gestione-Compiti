package it.unimol.newunimol.gestionecompiti.dto;

public record SubmissionRequestDto(
    long assignmentId,
    String studentId,
    String filePath
) {}
