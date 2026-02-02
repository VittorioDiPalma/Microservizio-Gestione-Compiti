package it.unimol.newunimol.gestionecompiti.messaging.listener;

import it.unimol.newunimol.gestionecompiti.messaging.event.AssessmentCreatedEvent;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AssessmentEventListener {

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Gestisce evento: valutazione creata
     * Permette di settare lo stato del compito a GRADED
     */
    @RabbitListener(queues = "${rabbitmq.queue.assessment.created}")
    @Transactional
    public void handleAssessmentCreated(AssessmentCreatedEvent event) {
        try {
            Optional<Submission> optSubmission = submissionRepository.findById(event.getSubmissionId());

            if (optSubmission.isEmpty()) {
                return;
            }

            Submission submission = optSubmission.get();

            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);

        } catch (Exception e) {
            throw e;
        }
    }
}
