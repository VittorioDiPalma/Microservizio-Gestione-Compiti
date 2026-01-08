package it.unimol.newunimol.gestionecompiti.dto.converter;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import org.springframework.stereotype.Component;

@Component
public class AssignmentConverter {
    
    public Assignment toEntity(AssignmentRequestDto dto){
        if(dto == null){
            return null;
        }

        Assignment entity = new Assignment();
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setDueDate(dto.dueDate());
        entity.setCourseId(dto.courseId());
        entity.setProfessorId(dto.professorId());
        entity.setAttachmentPath(dto.attachmentPath());

        return entity;
    }

    public AssignmentResponseDto toDto(Assignment entity){
        if(entity == null){
            return null;
        }

        return new AssignmentResponseDto(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getCreationDate(),
            entity.getDueDate(),
            entity.getAttachmentPath(),
            entity.getCourseId(),
            entity.getProfessorId()
        );
    }
}
