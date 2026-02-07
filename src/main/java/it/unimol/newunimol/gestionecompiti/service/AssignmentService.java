package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.client.GestioneCorsiClient;
import it.unimol.newunimol.gestionecompiti.client.GestioneIscrizioniClient;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentUpdateDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.AssignmentConverter;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import it.unimol.newunimol.gestionecompiti.messaging.publisher.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;

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

    @Autowired
    private GestioneCorsiClient corsiClient;

    @Autowired
    private GestioneIscrizioniClient iscrizioniClient;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Crea un nuovo compito (DOCENTE)
     * 
     * @param request   DTO con i dati del compito
     * @param teacherId ID del docente
     * 
     * @return DTO con i dati del compito creato
     */
    public AssignmentResponseDto createAssignment(AssignmentRequestDto request, String teacherId) {
        if (request.dueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La data di scadenza deve essere futura");
        }

        if (!corsiClient.courseExists(request.courseId())) {
            throw new IllegalArgumentException("Corso non trovato: " + request.courseId());
        }

        // verifica che il docente insegni il corso
        if (!SecurityUtils.isAdmin()
                && !corsiClient.isTeacherOfCourse(request.courseId(), teacherId)) {
            throw new AccessDeniedException(
                    "Non puoi creare compiti per questo corso. "
                            + "Non sei tra i docenti assegnati al corso: " + request.courseId());
        }

        Assignment assignment = assignmentConverter.toEntity(request);
        assignment.setTeacherId(teacherId);
        assignment = assignmentRepository.save(assignment);

        eventPublisher.publishAssignmentCreated(assignment);

        int totalSubmissions = 0;
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }

    /**
     * Trova un compito per ID
     * 
     * @param id       ID del compito
     * @param callerId ID dell'utente che effettua la chiamata
     * 
     * @return DTO con i dati del compito
     */
    public AssignmentResponseDto findById(String id, String callerId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));

        if (!SecurityUtils.isAdmin()) {
            if (!assignment.getTeacherId().equals(callerId)) {
                if (!iscrizioniClient.isStudentEnrolled(callerId, assignment.getCourseId())) {
                    throw new AccessDeniedException(
                            "Non sei autorizzato a visualizzare questo assignment. "
                                    + "Devi essere iscritto al corso: " + assignment.getCourseId());
                }
                throw new AccessDeniedException("Non sei autorizzato a visualizzare questo assignment");
            }
        }

        int totalSubmissions = submissionRepository.countByAssignmentId(id);
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }

    /**
     * Ottiene tutti i compiti di un corso
     * 
     * @param courseId ID del corso
     * @param callerId ID dell'utente che effettua la chiamata
     * 
     * @return Lista di DTO con i dati dei compiti
     */
    public List<AssignmentResponseDto> getAssignmentsByCourse(String courseId, String callerId) {
        // Controllo autorizzazione
        if (!SecurityUtils.isAdmin()) {
            boolean isTeacher = corsiClient.isTeacherOfCourse(courseId, callerId);

            if (!isTeacher) {
                boolean isEnrolled = iscrizioniClient.isStudentEnrolled(callerId, courseId);

                if (!isEnrolled) {
                    throw new AccessDeniedException(
                            "Non sei autorizzato a visualizzare i compiti di questo corso. "
                                    + "Devi essere docente del corso o studente iscritto.");
                }
            }
        }

        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);

        return assignments.stream()
                .map(assignment -> {
                    int totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
                    return assignmentConverter.toDto(assignment, totalSubmissions);
                })
                .collect(Collectors.toList());
    }

    /**
     * Modifica un compito esistente (DOCENTE)
     * Verifica che il docente sia il proprietario del compito
     * Campi modificabili: title, description, dueDate
     * Campi NON modificabili: courseId, teacherId
     * 
     * @param id        ID del compito da modificare
     * @param request   DTO con i dati aggiornati
     * @param teacherId ID del docente che effettua la modifica
     * 
     * @return DTO con i dati del compito aggiornato
     */
    public AssignmentResponseDto updateAssignment(String id, AssignmentUpdateDto request, String teacherId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));

        if (!SecurityUtils.isAdmin() && !assignment.getTeacherId().equals(teacherId)) {
            throw new AccessDeniedException("Non hai i permessi per modificare questo compito");
        }

        if (request.dueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La data di scadenza deve essere futura");
        }

        assignmentConverter.updateEntity(assignment, request);

        assignment = assignmentRepository.save(assignment);

        eventPublisher.publishAssignmentUpdated(assignment);

        int totalSubmissions = submissionRepository.countByAssignmentId(id);
        return assignmentConverter.toDto(assignment, totalSubmissions);
    }

    /**
     * Elimina un compito (DOCENTE)
     * 
     * @param id        ID del compito da eliminare
     * @param teacherId ID del docente che effettua l'eliminazione
     */
    public void deleteAssignmentById(String id, String teacherId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compito non trovato con ID: " + id));

        if (!SecurityUtils.isAdmin() && !assignment.getTeacherId().equals(teacherId)) {
            throw new AccessDeniedException("Non hai i permessi per eliminare questo compito");
        }

        eventPublisher.publishAssignmentDeleted(
                assignment.getId(),
                assignment.getCourseId(),
                assignment.getTeacherId(),
                assignment.getTitle());

        List<Submission> submissions = submissionRepository.findByAssignmentId(id);

        for (Submission submission : submissions) {
            fileStorageService.deleteAllFilesByEntity(
                    FileEntityType.SUBMISSION,
                    submission.getId());
        }

        fileStorageService.deleteAllFilesByEntity(
                FileEntityType.ASSIGNMENT,
                id);

        submissionRepository.deleteByAssignmentId(id);

        assignmentRepository.deleteById(id);
    }
}