package it.unimol.newunimol.gestionecompiti.dto.converter;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubmissionConverter {

    public Submission toEntity(SubmissionRequestDto dto){
        if(dto == null){
            return null;
        }
        
        Submission entity = new Submission();
        entity.setAssignmentId(dto.assignmentId());
        entity.setStudentId(dto.studentId());
        entity.setFilePath(dto.filePath());
        
        return entity;
    }

    public SubmissionResponseDto toDto(Submission entity){
        if(entity == null){
            return null;
        }

        return new SubmissionResponseDto(
            entity.getId(),
            entity.getAssignmentId(),
            entity.getStudentId(),
            entity.getFilePath(),
            entity.getSubmissionDate(),
            entity.getStatus(),
            entity.getGrade(),
            entity.getFeedback()
        );
    }
    
}
