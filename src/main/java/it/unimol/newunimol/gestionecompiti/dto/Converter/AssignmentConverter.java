package it.unimol.newunimol.gestionecompiti.dto.converter;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import org.springframework.stereotype.Component;

@Component
public class AssignmentConverter {
    
    public Assignment toEntity(AssignmentRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDescription(dto.description());
        assignment.setDueDate(dto.dueDate());
        assignment.setCourseId(dto.courseId());
        assignment.setTeacherId(dto.teacherId());
        assignment.setAttachments(dto.attachments() != null ? dto.attachments() : java.util.List.of());
        return assignment;
    }
    
    public AssignmentResponseDto toDto(Assignment assignment, int totalSubmissions) {
        return new AssignmentResponseDto(
            assignment.getId(),
            assignment.getTitle(),
            assignment.getDescription(),
            assignment.getDueDate(),
            assignment.getCourseId(),
            assignment.getTeacherId(),
            assignment.getAttachments(),
            assignment.getCreatedAt(),
            assignment.getUpdatedAt(),
            totalSubmissions
        );
    }
    
    //Without total submissions
    public AssignmentResponseDto toDto(Assignment assignment) {
        return toDto(assignment, 0);
    }
}