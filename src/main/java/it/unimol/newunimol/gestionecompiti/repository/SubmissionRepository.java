package it.unimol.newunimol.gestionecompiti.repository;

import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    
    int countByAssignmentId(String assignmentId);
    
    void deleteByAssignmentId(String assignmentId);
    
    List<Submission> findByAssignmentId(String assignmentId);
    
    Optional<Submission> findByAssignmentIdAndStudentId(String assignmentId, String studentId);
    
    List<Submission> findByStudentId(String studentId);
    
    int countByAssignmentIdAndStatus(String assignmentId, SubmissionStatus status);
    
    List<Submission> findByAssignmentIdAndStatus(String assignmentId, SubmissionStatus status);
    
    boolean existsByAssignmentIdAndStudentId(String assignmentId, String studentId);
}