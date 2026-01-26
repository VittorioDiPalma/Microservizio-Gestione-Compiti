package it.unimol.newunimol.gestionecompiti.dto.converter;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import org.springframework.stereotype.Component;


@Component
public class SubmissionConverter {
    
    public Submission toEntity(SubmissionRequestDto dto) {
        Submission submission = new Submission();
        submission.setContent(dto.content());
        return submission;
    }
    
    public SubmissionResponseDto toDto(Submission submission) {
        return new SubmissionResponseDto(
            submission.getId(),
            submission.getAssignmentId(),
            submission.getStudentId(),
            submission.getContent(),
            java.util.List.of(),
            submission.getSubmittedAt(),
            submission.getStatus()
        );
    }
}