package it.unimol.newunimol.gestionecompiti.dto;

import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import java.time.LocalDateTime;

public record SubmissionResponseDto(
    String id,
    String assignmentId,
    String studentId,
    String filePath,
    LocalDateTime submissionDate,
    SubmissionStatus status
) {}
