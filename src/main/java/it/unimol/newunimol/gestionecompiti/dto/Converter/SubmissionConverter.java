package it.unimol.newunimol.gestionecompiti.dto.converter;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubmissionConverter {
    
    public Submission toEntity(SubmissionRequestDto dto) {
        Submission submission = new Submission();
        submission.setAssignmentId(dto.assignmentId());
        submission.setStudentId(dto.studentId());
        submission.setContent(dto.content());
        submission.setAttachments(dto.attachments() != null ? dto.attachments() : java.util.List.of());
        submission.setSubmittedAt(LocalDateTime.now()); // ← Calcolato automaticamente
        // Lo status viene impostato dal Service dopo aver controllato la scadenza
        return submission;
    }
    
    public SubmissionResponseDto toDto(Submission submission) {
        return new SubmissionResponseDto(
            submission.getId(),
            submission.getAssignmentId(),
            submission.getStudentId(),
            submission.getContent(),
            submission.getAttachments(),
            submission.getSubmittedAt(),
            submission.getStatus()
        );
    }
}