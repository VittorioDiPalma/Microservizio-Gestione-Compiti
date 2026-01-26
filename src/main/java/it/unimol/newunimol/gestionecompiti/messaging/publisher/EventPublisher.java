package it.unimol.newunimol.gestionecompiti.messaging.publisher;

import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisher {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.name:newunimol.events}")
    private String exchangeName;
    
    /**
     * Pubblica evento: assignment creato
     */
    public void publishAssignmentCreated(Assignment assignment) {
        Map<String, Object> message = createAssignmentMessage(assignment, "ASSIGNMENT_CREATED");
        rabbitTemplate.convertAndSend(exchangeName, "assignment.created", message);
    }
    
    /**
     * Pubblica evento: assignment aggiornato
     */
    public void publishAssignmentUpdated(Assignment assignment) {
        Map<String, Object> message = createAssignmentMessage(assignment, "ASSIGNMENT_UPDATED");
        rabbitTemplate.convertAndSend(exchangeName, "assignment.updated", message);
    }
    
    /**
     * Pubblica evento: assignment eliminato
     */
    public void publishAssignmentDeleted(String assignmentId, String courseId, String teacherId, String title) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "ASSIGNMENT_DELETED");
        message.put("assignmentId", assignmentId);
        message.put("courseId", courseId);
        message.put("teacherId", teacherId);
        message.put("title", title);
        message.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(exchangeName, "assignment.deleted", message);
    }
    
    /**
     * Pubblica evento: submission creata
     */
    public void publishSubmissionCreated(Submission submission, String courseId, boolean isLate) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "SUBMISSION_CREATED");
        message.put("submissionId", submission.getId());
        message.put("assignmentId", submission.getAssignmentId());
        message.put("studentId", submission.getStudentId());
        message.put("courseId", courseId);
        message.put("status", submission.getStatus().toString());
        message.put("isLate", isLate);
        message.put("submittedAt", submission.getSubmittedAt().toString());
        message.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(exchangeName, "submission.created", message);
    }
    
    /**
     * Helper per creare messaggi assignment
     */
    private Map<String, Object> createAssignmentMessage(Assignment assignment, String eventType) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", eventType);
        message.put("assignmentId", assignment.getId());
        message.put("courseId", assignment.getCourseId());
        message.put("teacherId", assignment.getTeacherId());
        message.put("title", assignment.getTitle());
        message.put("description", assignment.getDescription());
        message.put("dueDate", assignment.getDueDate() != null ? assignment.getDueDate().toString() : null);
        message.put("createdAt", assignment.getCreatedAt() != null ? assignment.getCreatedAt().toString() : null);
        message.put("updatedAt", assignment.getUpdatedAt() != null ? assignment.getUpdatedAt().toString() : null);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }
}
