package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.SubmissionConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionConverter submissionConverter;
    
    /**
     * This method creates a new submission based on the provided request data.
     * Additionally, it sets the submission date and status depending on whether 
     * the submission is late or on time.
     * 
     * @param request
     * @return The created submission data.
     * @throws IllegalArgumentException if the request data is invalid.
     */
    public SubmissionResponseDto createSubmission(SubmissionRequestDto request) {
        if (request == null ||
            request.assignmentId() == null || request.assignmentId().isEmpty() ||
            request.studentId() == null || request.studentId().isEmpty()) {
            throw new IllegalArgumentException("Dati della consegna non validi");
        }

        Assignment assignment = assignmentRepository.findById(request.assignmentId())
                .orElseThrow(() -> new EntityNotFoundException
                ("Compito non trovato con ID: " + request.assignmentId()));

        Submission submission = submissionConverter.toEntity(request);

        LocalDateTime now = LocalDateTime.now();
        submission.setSubmissionDate(now);

        if (now.toLocalDate().isAfter(assignment.getDueDate())) {
            submission.setStatus(SubmissionStatus.LATE);
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED);
        }

        return submissionConverter
                .toDto(submissionRepository.save(submission));
    }

    /**
     * This method retrieves all submissions for a specific assignment.
     * 
     * @param assignmentId The ID of the assignment.
     * @return A list of submission response DTOs.
     */
    public List<SubmissionResponseDto> getSubmissionsByAssignment(String assignmentId) {
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException
                ("ID del compito non valido");
        }

        return submissionRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(submissionConverter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * This method retrieves a student's submission for a specific assignment.
     * 
     * @param assignmentId The ID of the assignment.
     * @param studentId The ID of the student.
     * @return The submission response DTO for the student's submission.
     */
    public SubmissionResponseDto getStudentSubmission(String assignmentId, String studentId) {
        if (assignmentId == null ||
            assignmentId.isEmpty() ||
            studentId == null ||
            studentId.isEmpty()) {
            throw new IllegalArgumentException
                ("Dati non validi per la ricerca della consegna");
            }
            
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(submissionConverter::toDto)
                .orElseThrow(() -> new EntityNotFoundException
                    ("Nessuna consegna trovata per lo studente " + studentId));
    }

}