package it.unimol.newunimol.gestionecompiti.controller;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.service.SubmissionService;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/submissions")
@Tag(name = "Submission", description = "API per la gestione delle consegne")
public class SubmissionController {
    
    @Autowired
    private SubmissionService submissionService;
    
    /**
     * Crea una nuova consegna (STUDENTE)
     */
    @PostMapping
    @Operation(summary = "Crea una nuova consegna", description = "Permette a uno studente di consegnare un compito")
    public ResponseEntity<SubmissionResponseDto> createSubmission(@Valid @RequestBody SubmissionRequestDto request) {
        SubmissionResponseDto response = submissionService.createSubmission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Ottiene tutte le consegne di un assignment (DOCENTE)
     */
    @GetMapping("/assignment/{assignmentId}")
    @Operation(summary = "Ottiene le consegne di un compito", description = "Restituisce tutte le consegne per un compito specifico")
    public ResponseEntity<List<SubmissionResponseDto>> getSubmissionsByAssignment(@PathVariable String assignmentId) {
        List<SubmissionResponseDto> response = submissionService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ottiene le statistiche delle consegne per un assignment (NUOVO - DOCENTE)
     */
    @GetMapping("/assignment/{assignmentId}/stats")
    @Operation(summary = "Statistiche consegne", description = "Restituisce statistiche sulle consegne di un compito")
    public ResponseEntity<SubmissionStatsDto> getSubmissionStats(@PathVariable String assignmentId) {
        SubmissionStatsDto stats = submissionService.getSubmissionStats(assignmentId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Ottiene solo le consegne in ritardo per un assignment (NUOVO - DOCENTE)
     */
    @GetMapping("/assignment/{assignmentId}/late")
    @Operation(summary = "Consegne in ritardo", description = "Restituisce le consegne in ritardo per un compito")
    public ResponseEntity<List<SubmissionResponseDto>> getLateSubmissions(@PathVariable String assignmentId) {
        List<SubmissionResponseDto> response = submissionService.getLateSubmissions(assignmentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ottiene la consegna di uno studente per un assignment (STUDENTE + DOCENTE)
     */
    @GetMapping("/assignment/{assignmentId}/student/{studentId}")
    @Operation(summary = "Ottiene la consegna di uno studente", description = "Restituisce la consegna di uno studente specifico per un compito")
    public ResponseEntity<SubmissionResponseDto> getStudentSubmission(
            @PathVariable String assignmentId,
            @PathVariable String studentId) {
        SubmissionResponseDto response = submissionService.getStudentSubmission(assignmentId, studentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ottiene tutte le consegne di uno studente (STUDENTE)
     */
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Ottiene tutte le consegne di uno studente", description = "Restituisce tutte le consegne effettuate da uno studente")
    public ResponseEntity<List<SubmissionResponseDto>> getSubmissionsByStudent(@PathVariable String studentId) {
        List<SubmissionResponseDto> response = submissionService.getSubmissionsByStudent(studentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Modifica una consegna (STUDENTE - solo prima della scadenza)
     */
    @PutMapping("/{submissionId}")
    @Operation(summary = "Modifica una consegna", description = "Permette a uno studente di modificare una consegna prima della scadenza")
    public ResponseEntity<SubmissionResponseDto> updateSubmission(
            @PathVariable String submissionId,
            @Valid @RequestBody SubmissionRequestDto request) {
        SubmissionResponseDto response = submissionService.updateSubmission(submissionId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Elimina una consegna
     */
    @DeleteMapping("/{submissionId}")
    @Operation(summary = "Elimina una consegna", description = "Permette di eliminare una consegna")
    public ResponseEntity<Void> deleteSubmission(@PathVariable String submissionId) {
        boolean deleted = submissionService.deleteSubmission(submissionId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}