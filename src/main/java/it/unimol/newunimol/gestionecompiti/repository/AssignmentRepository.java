package it.unimol.newunimol.gestionecompiti.repository;

import it.unimol.newunimol.gestionecompiti.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    
    List<Assignment> findByCourseId(String courseId);
    
    List<Assignment> findByTeacherId(String teacherId);
    
    // Trova compiti in scadenza (tra ora e N giorni dopo)
    @Query("SELECT a FROM Assignment a WHERE a.courseId = :courseId AND a.dueDate BETWEEN :now AND :futureDate")
    List<Assignment> findUpcomingAssignmentsByCourse(
        @Param("courseId") String courseId, 
        @Param("now") LocalDateTime now, 
        @Param("futureDate") LocalDateTime futureDate
    );
    
    // Trova compiti per corso con scadenza futura
    @Query("SELECT a FROM Assignment a WHERE a.courseId = :courseId AND a.dueDate > :now")
    List<Assignment> findActiveByCourseId(@Param("courseId") String courseId, @Param("now") LocalDateTime now);
}