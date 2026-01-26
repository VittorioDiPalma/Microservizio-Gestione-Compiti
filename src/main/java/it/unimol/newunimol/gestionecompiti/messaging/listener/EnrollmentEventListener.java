package it.unimol.newunimol.gestionecompiti.messaging.listener;

import it.unimol.newunimol.gestionecompiti.messaging.event.StudentUnenrolledEvent;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.service.FileStorageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class EnrollmentEventListener {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Gestisce evento: studente disiscritto da un corso
     * Elimina tutte le consegne dello studente per i compiti di quel corso (e i
     * relativi file)
     */
    @RabbitListener(queues = "${rabbitmq.queue.student.unenrolled}")
    @Transactional
    public void handleStudentUnenrolled(StudentUnenrolledEvent event) {
        try {
            List<Assignment> assignments = assignmentRepository.findByCourseId(event.getCourseId());

            if (assignments.isEmpty()) {
                return;
            }

            for (Assignment assignment : assignments) {
                Optional<Submission> optSubmission = submissionRepository
                        .findByAssignmentIdAndStudentId(assignment.getId(), event.getStudentId());

                if (optSubmission.isPresent()) {
                    Submission submission = optSubmission.get();

                    fileStorageService.deleteAllFilesByEntity(
                            FileEntityType.SUBMISSION,
                            submission.getId());

                    submissionRepository.delete(submission);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
