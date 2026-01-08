package it.unimol.newunimol.gestionecompiti.repository;

import it.unimol.newunimol.gestionecompiti.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {

    List<Assignment> findByCourseId(String courseId);

    List<Assignment> findByProfessorId(String professorId);

    List<Assignment> findByCreationDate(LocalDate date);

    List<Assignment> findByCreationDateAfter(LocalDate date);

    // Trova i compiti di un corso ordinati per data di creazione decrescente
    // (Così l'ultimo creato appare per primo)
    List<Assignment> findByCourseIdOrderByCreationDateDesc(String courseId);
}