package it.unimol.newunimol.gestionecompiti.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SubmissionRequestDto(
    @NotBlank(message = "L'ID dell'assignment è obbligatorio")
    String assignmentId,
    
    @NotBlank(message = "L'ID dello studente è obbligatorio")
    String studentId,
    
    String content,
    
    List<String> attachments
) {}