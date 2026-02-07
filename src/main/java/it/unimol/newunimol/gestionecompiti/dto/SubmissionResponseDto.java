package it.unimol.newunimol.gestionecompiti.dto;

import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponseDto(
    String id,
    String assignmentId,
    String studentId,
    String content,
    List<String> attachments,
    LocalDateTime submittedAt,
    SubmissionStatus status
) { }