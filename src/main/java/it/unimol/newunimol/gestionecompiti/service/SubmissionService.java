package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.client.GestioneIscrizioniClient;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionStatsDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.SubmissionConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import it.unimol.newunimol.gestionecompiti.messaging.publisher.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GestioneIscrizioniClient iscrizioniClient;

    @Autowired
    private EventPublisher eventPublisher;

    private void verifyAssignmentOwnership(String assignmentId, String teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + assignmentId));

        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new AccessDeniedException(
                    "Non puoi accedere alle consegne di questo compito. " +
                            "Non sei il proprietario dell'assignment.");
        }
    }

    /**
     * Crea una nuova consegna (STUDENTE)
     * 
     * @param request      DTO con contenuto della consegna
     * @param assignmentId ID del compito
     * @param studentId    ID dello studente
     */
    public SubmissionResponseDto createSubmission(SubmissionRequestDto request, String assignmentId, String studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + assignmentId));

        if (!SecurityUtils.isAdmin()) {
            boolean isEnrolled = iscrizioniClient.isStudentEnrolled(studentId, assignment.getCourseId());
            if (!isEnrolled) {
                throw new AccessDeniedException(
                        "Non puoi consegnare questo compito. " +
                                "Devi essere iscritto al corso: " + assignment.getCourseId());
            }
        }

        // verifica che lo studente non abbia già consegnato
        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Hai già consegnato questo compito");
                });

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setContent(request.content());
        submission.setSubmittedAt(LocalDateTime.now());

        // imposta status in base alla scadenza
        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(assignment.getDueDate());
        if (isLate) {
            submission.setStatus(SubmissionStatus.LATE);
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED);
        }

        submission = submissionRepository.save(submission);

        eventPublisher.publishSubmissionCreated(submission, assignment.getCourseId(), isLate);

        return submissionConverter.toDto(submission);
    }

    /**
     * Ottiene tutte le consegne di un assignment (DOCENTE)
     * 
     * @param assignmentId ID del compito
     * 
     * @return Lista di SubmissionResponseDto
     */
    public List<SubmissionResponseDto> getSubmissionsByAssignment(String assignmentId) {
        String teacherId = SecurityUtils.getCurrentUserId();
        verifyAssignmentOwnership(assignmentId, teacherId);

        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);

        return submissions.stream()
                .map(submissionConverter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Ottiene statistiche delle consegne per un assignment (DOCENTE)
     * 
     * @param assignmentId ID del compito
     * 
     * @return SubmissionStatsDto con le statistiche per il compito richiesto
     */
    public SubmissionStatsDto getSubmissionStats(String assignmentId) {
        String teacherId = SecurityUtils.getCurrentUserId();
        verifyAssignmentOwnership(assignmentId, teacherId);

        // statistiche sulle submission
        int totalSubmissions = submissionRepository.countByAssignmentId(assignmentId);
        int onTimeSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId,
                SubmissionStatus.SUBMITTED);
        int lateSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.LATE);
        int gradedSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId,
                SubmissionStatus.GRADED);
        int pendingSubmissions = submissionRepository.countByAssignmentIdAndStatus(assignmentId,
                SubmissionStatus.PENDING);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));

        List<String> enrolledStudentIds = iscrizioniClient.getEnrolledStudentIds(assignment.getCourseId());
        int totalEnrolledStudents = enrolledStudentIds.size();

        List<Submission> allSubmissions = submissionRepository.findByAssignmentId(assignmentId);
        long studentsSubmitted = allSubmissions.stream()
                .map(Submission::getStudentId)
                .distinct()
                .count();

        int studentsMissing = totalEnrolledStudents - (int) studentsSubmitted;

        // calcola percentuale di completamento
        double completionRate = totalEnrolledStudents > 0
                ? (studentsSubmitted * 100.0) / totalEnrolledStudents
                : 0.0;

        return new SubmissionStatsDto(
                assignmentId,
                totalSubmissions,
                onTimeSubmissions,
                lateSubmissions,
                gradedSubmissions,
                pendingSubmissions,
                totalEnrolledStudents,
                studentsMissing,
                Math.round(completionRate * 100.0) / 100.0);
    }

    /**
     * Ottiene la consegna di uno studente specifico per un assignment (DOCENTE)
     * 
     * @param assignmentId ID del compito
     * @param studentId    ID dello studente
     * @param callerId     ID del chiamante
     * 
     * @return SubmissionResponseDto della consegna richiesta
     */
    public SubmissionResponseDto getStudentSubmission(String assignmentId, String studentId, String callerId) {
        Submission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Consegna non trovata per assignment: " + assignmentId + " e studente: " + studentId));

        if (!SecurityUtils.isAdmin()) {
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + assignmentId));

            if (!assignment.getTeacherId().equals(callerId)) {
                throw new AccessDeniedException(
                        "Non hai i permessi per vedere questa consegna. " +
                                "Non sei il proprietario dell'assignment.");
            }
        }

        return submissionConverter.toDto(submission);
    }

    /**
     * Ottiene una consegna specifica per ID (STUDENTE + DOCENTE)
     * - STUDENTE: può vedere solo le proprie consegne
     * - DOCENTE: può vedere solo consegne dei propri assignment
     * 
     * @param submissionId ID della consegna
     * @param callerId     ID del chiamante
     * 
     * @return SubmissionResponseDto della consegna richiesta
     */
    public SubmissionResponseDto getSubmissionById(String submissionId, String callerId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Consegna non trovata con ID: " + submissionId));

        if (!SecurityUtils.isAdmin()) {
            if (SecurityUtils.hasRole("ROLE_STUDENT")) {
                if (!submission.getStudentId().equals(callerId)) {
                    throw new AccessDeniedException(
                            "Non hai i permessi per vedere questa consegna. Puoi vedere solo le tue consegne.");
                }
            } else if (SecurityUtils.hasRole("ROLE_TEACHER")) {
                // docente può vedere solo consegne dei propri assignment
                Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                        .orElseThrow(() -> new EntityNotFoundException("Compito associato non trovato"));

                if (!assignment.getTeacherId().equals(callerId)) {
                    throw new AccessDeniedException(
                            "Non hai i permessi per vedere questa consegna. " +
                                    "Non sei il proprietario dell'assignment associato.");
                }
            }
        }

        return submissionConverter.toDto(submission);
    }

    /**
     * Modifica una consegna esistente (STUDENTE - solo se non scaduto e non
     * valutata)
     * 
     * @param submissionId ID della consegna
     * @param request      DTO con i nuovi dati della consegna
     * @param studentId    ID dello studente
     * 
     * @return SubmissionResponseDto della consegna aggiornata
     */
    public SubmissionResponseDto updateSubmission(String submissionId, SubmissionRequestDto request, String studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Consegna non trovata con ID: " + submissionId));

        if (!SecurityUtils.isAdmin() && !submission.getStudentId().equals(studentId)) {
            throw new AccessDeniedException("Non hai i permessi per modificare questa consegna");
        }

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new IllegalArgumentException(
                    "Non puoi modificare una consegna già valutata. " +
                            "Contatta il docente se hai bisogno di modifiche.");
        }

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(assignment.getDueDate())) {
            throw new IllegalArgumentException("Non è possibile modificare una consegna dopo la scadenza");
        }

        submission.setContent(request.content());

        submission = submissionRepository.save(submission);
        return submissionConverter.toDto(submission);
    }

    /**
     * Elimina una consegna (STUDENTE), ed eventuali file associati
     * 
     * @param submissionId ID della consegna
     * @param studentId    ID dello studente
     */
    public void deleteSubmission(String submissionId, String studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Consegna non trovata con ID: " + submissionId));

        if (!SecurityUtils.isAdmin() && !submission.getStudentId().equals(studentId)) {
            throw new AccessDeniedException("Non hai i permessi per eliminare questa consegna");
        }

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new IllegalArgumentException(
                    "Non puoi eliminare una consegna già valutata. " +
                            "Contatta il docente se hai bisogno di modifiche.");
        }

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new EntityNotFoundException("Compito associato non trovato"));

        if (LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new IllegalArgumentException(
                    "Non puoi eliminare una consegna dopo la scadenza del compito.");
        }

        fileStorageService.deleteAllFilesByEntity(
                FileEntityType.SUBMISSION,
                submissionId);

        submissionRepository.deleteById(submissionId);
    }

    /**
     * Conta le consegne per un assignment
     */
    public int countSubmissionsByAssignment(String assignmentId) {
        return submissionRepository.countByAssignmentId(assignmentId);
    }
}