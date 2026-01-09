package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.AssignmentConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
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
public class AssignmentService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private AssignmentConverter assignmentConverter;
    
    /**
     * Crea un nuovo compito (DOCENTE)
     */
    public AssignmentResponseDto createAssignment(AssignmentRequestDto request) {
        if (request.dueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La data di scadenza deve essere futura");
        }
        
        Assignment assignment = assignmentConverter.toEntity(request);
        assignment = assignmentRepository.save(assignment);
        
        int totalSubmissions = 0;
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }
    
    /**
     * Trova un compito per ID
     */
    public AssignmentResponseDto findById(String id) {
        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));
        
        int totalSubmissions = submissionRepository.countByAssignmentId(id);
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }
    
    /**
     * Ottiene tutti i compiti di un corso (STUDENTI + DOCENTI)
     */
    public List<AssignmentResponseDto> getAssignmentsByCourse(String courseId) {
        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        
        return assignments.stream()
            .map(assignment -> {
                int totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
                return assignmentConverter.toDto(assignment, totalSubmissions);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Ottiene tutti i compiti di un docente (DOCENTE)
     */
    public List<AssignmentResponseDto> getAssignmentsByTeacher(String teacherId) {
        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        
        return assignments.stream()
            .map(assignment -> {
                int totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
                return assignmentConverter.toDto(assignment, totalSubmissions);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Ottiene i compiti in scadenza per uno studente (NUOVO)
     * Nota: Assumiamo che lo studente sia iscritto ai corsi, 
     * quindi prendiamo i compiti dei suoi corsi in scadenza
     */
    public List<AssignmentResponseDto> getUpcomingAssignments(String studentId, int days) {
        // TODO: In produzione, dovresti chiamare il microservizio "Gestione Iscrizioni"
        // per ottenere i corsi dello studente. Per ora usiamo una logica semplificata.
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        
        // Query personalizzata: trova tutti gli assignment in scadenza
        // Per ora restituiamo tutti gli assignment in scadenza (semplificazione)
        List<Assignment> allAssignments = assignmentRepository.findAll();
        
        return allAssignments.stream()
            .filter(a -> a.getDueDate().isAfter(now) && a.getDueDate().isBefore(futureDate))
            .filter(a -> !submissionRepository.existsByAssignmentIdAndStudentId(a.getId(), studentId))
            .map(assignment -> {
                int totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
                return assignmentConverter.toDto(assignment, totalSubmissions);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Ottiene i compiti che lo studente deve ancora consegnare (NUOVO)
     */
    public List<AssignmentResponseDto> getPendingAssignments(String studentId) {
        // TODO: In produzione, filtrare per corsi dello studente
        LocalDateTime now = LocalDateTime.now();
        
        List<Assignment> allAssignments = assignmentRepository.findAll();
        
        return allAssignments.stream()
            .filter(a -> a.getDueDate().isAfter(now)) // Solo compiti non scaduti
            .filter(a -> !submissionRepository.existsByAssignmentIdAndStudentId(a.getId(), studentId))
            .map(assignment -> {
                int totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
                return assignmentConverter.toDto(assignment, totalSubmissions);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Modifica un compito esistente (DOCENTE)
     */
    public AssignmentResponseDto updateAssignment(String id, AssignmentRequestDto request) {
        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));
        
        if (request.dueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La data di scadenza deve essere futura");
        }
        
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setAttachments(request.attachments() != null ? request.attachments() : List.of());
        
        assignment = assignmentRepository.save(assignment);
        
        int totalSubmissions = submissionRepository.countByAssignmentId(id);
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }
    
    /**
     * Elimina un compito (DOCENTE)
     */
    public boolean deleteAssignmentById(String id) {
        if (!assignmentRepository.existsById(id)) {
            return false;
        }
        
        submissionRepository.deleteByAssignmentId(id);
        assignmentRepository.deleteById(id);
        return true;
    }
}