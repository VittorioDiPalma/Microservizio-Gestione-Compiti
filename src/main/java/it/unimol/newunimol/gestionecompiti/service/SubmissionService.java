package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionStatsDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.SubmissionConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Crea una nuova consegna (STUDENTE)
     */
    public SubmissionResponseDto createSubmission(SubmissionRequestDto request) {
        Assignment assignment = assignmentRepository.findById(request.assignmentId())
            .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + request.assignmentId()));
        
        submissionRepository.findByAssignmentIdAndStudentId(request.assignmentId(), request.studentId())
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Lo studente ha già consegnato questo compito");
            });
        
        Submission submission = submissionConverter.toEntity(request);
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(assignment.getDueDate())) {
            submission.setStatus(SubmissionStatus.LATE);
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED);
        }
        
        submission = submissionRepository.save(submission);
        return submissionConverter.toDto(submission);
    }
    
    /**
     * Ottiene tutte le consegne di un assignment (DOCENTE)
     */
    public List<SubmissionResponseDto> getSubmissionsByAssignment(String assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new EntityNotFoundException("Compito non trovato con ID: " + assignmentId);
        }
        
        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        
        return submissions.stream()
            .map(submissionConverter::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Ottiene statistiche delle consegne per un assignment (NUOVO - DOCENTE)
     */
    public SubmissionStatsDto getSubmissionStats(String assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new EntityNotFoundException("Compito non trovato con ID: " + assignmentId);
        }
        
        int totalSubmissions = submissionRepository.countByAssignmentId(assignmentId);
        int onTimeSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.SUBMITTED);
        int lateSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.LATE);
        
        return new SubmissionStatsDto(
            assignmentId,
            totalSubmissions,
            onTimeSubmissions,
            lateSubmissions
        );
    }
    
    /**
     * Ottiene solo le consegne in ritardo (NUOVO - DOCENTE)
     */
    public List<SubmissionResponseDto> getLateSubmissions(String assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new EntityNotFoundException("Compito non trovato con ID: " + assignmentId);
        }
        
        List<Submission> lateSubmissions = submissionRepository.findByAssignmentIdAndStatus(
            assignmentId, 
            SubmissionStatus.LATE
        );
        
        return lateSubmissions.stream()
            .map(submissionConverter::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Ottiene la consegna di uno studente specifico per un assignment (STUDENTE + DOCENTE)
     */
    public SubmissionResponseDto getStudentSubmission(String assignmentId, String studentId) {
        Submission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Consegna non trovata per assignment: " + assignmentId + " e studente: " + studentId));
        
        return submissionConverter.toDto(submission);
    }
    
    /**
     * Ottiene tutte le consegne di uno studente (STUDENTE)
     */
    public List<SubmissionResponseDto> getSubmissionsByStudent(String studentId) {
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        
        return submissions.stream()
            .map(submissionConverter::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Modifica una consegna esistente (STUDENTE - solo se non scaduto)
     */
    public SubmissionResponseDto updateSubmission(String submissionId, SubmissionRequestDto request) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new EntityNotFoundException("Consegna non trovata con ID: " + submissionId));
        
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
            .orElseThrow(() -> new EntityNotFoundException("Compito non trovato"));
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(assignment.getDueDate())) {
            throw new IllegalArgumentException("Non è possibile modificare una consegna dopo la scadenza");
        }
        
        submission.setContent(request.content());
        submission.setAttachments(request.attachments() != null ? request.attachments() : List.of());
        submission.setSubmittedAt(LocalDateTime.now());
        
        submission = submissionRepository.save(submission);
        return submissionConverter.toDto(submission);
    }
    
    /**
     * Elimina una consegna (STUDENTE - solo se non scaduto, o DOCENTE)
     */
    public boolean deleteSubmission(String submissionId) {
        if (!submissionRepository.existsById(submissionId)) {
            return false;
        }
        
        submissionRepository.deleteById(submissionId);
        return true;
    }
    
    /**
     * Conta le consegne per un assignment
     */
    public int countSubmissionsByAssignment(String assignmentId) {
        return submissionRepository.countByAssignmentId(assignmentId);
    }
}