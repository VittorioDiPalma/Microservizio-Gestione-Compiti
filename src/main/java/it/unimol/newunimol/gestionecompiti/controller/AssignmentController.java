package it.unimol.newunimol.gestionecompiti.controller;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Assignment", description = "API per la gestione dei compiti")
public class AssignmentController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    /**
     * Crea un nuovo compito (DOCENTE)
     */
    @PostMapping
    @Operation(summary = "Crea un nuovo compito", description = "Permette a un docente di creare un nuovo compito")
    public ResponseEntity<AssignmentResponseDto> createAssignment(@Valid @RequestBody AssignmentRequestDto request) {
        AssignmentResponseDto response = assignmentService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Ottiene un compito per ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Ottiene un compito per ID", description = "Restituisce i dettagli di un compito specifico")
    public ResponseEntity<AssignmentResponseDto> getAssignmentById(@PathVariable String id) {
        AssignmentResponseDto response = assignmentService.findById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ottiene tutti i compiti di un corso
     */
    @GetMapping("/course/{courseId}")
    @Operation(summary = "Ottiene i compiti di un corso", description = "Restituisce tutti i compiti di un corso specifico")
    public ResponseEntity<List<AssignmentResponseDto>> getAssignmentsByCourse(@PathVariable String courseId) {
        List<AssignmentResponseDto> response = assignmentService.getAssignmentsByCourse(courseId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ottiene tutti i compiti di un docente
     */
    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "Ottiene i compiti di un docente", description = "Restituisce tutti i compiti creati da un docente")
    public ResponseEntity<List<AssignmentResponseDto>> getAssignmentsByTeacher(@PathVariable String teacherId) {
        List<AssignmentResponseDto> response = assignmentService.getAssignmentsByTeacher(teacherId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Modifica un compito esistente (DOCENTE)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifica un compito", description = "Permette a un docente di modificare un compito esistente")
    public ResponseEntity<AssignmentResponseDto> updateAssignment(
            @PathVariable String id,
            @Valid @RequestBody AssignmentRequestDto request) {
        AssignmentResponseDto response = assignmentService.updateAssignment(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Elimina un compito (DOCENTE)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un compito", description = "Permette a un docente di eliminare un compito")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String id) {
        boolean deleted = assignmentService.deleteAssignmentById(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}