package it.unimol.newunimol.gestionecompiti.dto;

import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import java.time.LocalDateTime;

public record SubmissionResponseDto(
    long id,
    long assignmentId,
    String studentId,
    String filePath,
    LocalDateTime submissionDate,
    SubmissionStatus status,
    int grade,
    String feedback
) {}
