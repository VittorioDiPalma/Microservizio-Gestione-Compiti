package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.AssignmentConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentConverter assignmentConverter;

    /**
     * This method creates a new assignment based on the provided request data.
     * 
     * @param request The data for the assignment to be created.
     * @throws IllegalArgumentException if the request data is invalid.
     */
    public AssignmentResponseDto createAssignment(AssignmentRequestDto request) {
        if (request == null ||
            request.title() == null || request.title().isEmpty() ||
            request.description() == null || request.description().isEmpty() ||
            request.dueDate() == null ||
            request.courseId() == null ||
            request.professorId() == null || request.professorId().isEmpty()
        ) {
            throw new IllegalArgumentException("Dati del compito non validi");
        }

        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La data di scadenza è già passata");
        }   

        if (request.attachmentPath() != null && request.attachmentPath().length() > 255) {
            throw new IllegalArgumentException("Il percorso dell'allegato è troppo lungo");
        }

        Assignment assignment = assignmentConverter.toEntity(request);
        assignment.setCreationDate(LocalDate.now());
        
        Assignment saved = assignmentRepository.save(assignment);
        return assignmentConverter.toDto(saved);
    }

    /**
     * This method retrieves an assignment by its ID.
     * 
     * @param id The ID of the assignment to retrieve.
     * @return The assignment data as a response DTO.
     * @throws EntityNotFoundException if no assignment with the given ID exists.
     */
    public AssignmentResponseDto findById(Long id) {
        return assignmentRepository.findById(id)
                .map(assignmentConverter::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));
    }

    /**
     * This method retrieves all assignments for a specific course.
     * 
     * @param courseId The ID of the course.
     * @return A list of assignment response DTOs.
     * @throws IllegalArgumentException if the course ID is null.
     */
    public List<AssignmentResponseDto> getAssignmentsByCourseId(Long courseId) {
        if (courseId == null){
            throw new IllegalArgumentException("ID del corso non valido");
        }

        return assignmentRepository.findByCourseId(courseId)
                .stream()
                .map(assignmentConverter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an assignment by its ID.
     * 
     * @param id The ID of the assignment to delete.
     * @return true if the assignment was deleted, false if it did not exist.
     */
    public boolean deleteAssignmentById(Long id) {
        if (assignmentRepository.existsById(id)) {
            assignmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * This method retrieves all assignments for a specific course, ordered by creation date descending.
     * 
     * @param courseId The ID of the course.
     * @return A list of assignment response DTOs.
     */
    public List<AssignmentResponseDto> getAssignmentsByCourse(Long courseId) {
        if(courseId == null) {
            throw new IllegalArgumentException("ID del corso non valido");
        }

        return assignmentRepository.findByCourseIdOrderByCreationDateDesc(courseId)
                .stream()
                .map(assignmentConverter::toDto)
                .collect(Collectors.toList());
    }
}