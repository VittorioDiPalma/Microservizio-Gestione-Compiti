package it.unimol.newunimol.gestionecompiti.messaging.listener;

import it.unimol.newunimol.gestionecompiti.messaging.event.CourseDeletedEvent;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.service.FileStorageService;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CourseEventListener {
        
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Gestisce evento: corso eliminato
     * Elimina tutti gli assignment (e relative submission) del corso
     */
    @RabbitListener(queues = "${rabbitmq.queue.course.deleted}")
    @Transactional
    public void handleCourseDeleted(CourseDeletedEvent event) {
        try {
            List<Assignment> assignments = assignmentRepository.findByCourseId(event.getCourseId());
            
            if (assignments.isEmpty()) {
                return;
            }
            
            for (Assignment assignment : assignments) {
                List<Submission> submissions = submissionRepository.findByAssignmentId(assignment.getId());
                
                for (Submission submission : submissions) {
                    fileStorageService.deleteAllFilesByEntity(
                        FileEntityType.SUBMISSION, 
                        submission.getId()
                    );
                }
                
                fileStorageService.deleteAllFilesByEntity(
                    FileEntityType.ASSIGNMENT, 
                    assignment.getId()
                );
                
                if (!submissions.isEmpty()) {
                    submissionRepository.deleteAll(submissions);
                }
            }
            
            assignmentRepository.deleteAll(assignments);
            
        } catch (Exception e) {
            throw e;
        }
    }
}
