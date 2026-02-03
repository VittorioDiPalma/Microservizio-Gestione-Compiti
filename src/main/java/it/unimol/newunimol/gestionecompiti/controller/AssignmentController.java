package it.unimol.newunimol.gestionecompiti.controller;

import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentUpdateDto;
import it.unimol.newunimol.gestionecompiti.service.AssignmentService;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Assignment", description = "API per la gestione dei compiti")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Crea un nuovo compito", description = "Permette a un docente di creare un nuovo compito. Per aggiungere file usa POST /api/v1/files/assignment/{id}/upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compito creato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dati non validi (validazione fallita, data scadenza passata)"),
            @ApiResponse(responseCode = "403", description = "Accesso negato (docente non insegna nel corso)"),
            @ApiResponse(responseCode = "404", description = "Corso non trovato")
    })
    public ResponseEntity<AssignmentResponseDto> createAssignment(
            @Valid @RequestBody AssignmentRequestDto request) {

        String teacherId = SecurityUtils.getCurrentUserId();
        AssignmentResponseDto response = assignmentService.createAssignment(request, teacherId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Ottiene un compito per ID", description = "Restituisce i dettagli di un compito. TEACHER può vedere solo i propri assignment. STUDENT può vedere assignment se iscritto al corso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compito trovato", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Compito non trovato"),
            @ApiResponse(responseCode = "403", description = "Accesso negato (non sei il proprietario o non sei iscritto al corso)")
    })
    public ResponseEntity<AssignmentResponseDto> getAssignmentById(@PathVariable String id) {

        String callerId = SecurityUtils.getCurrentUserId();
        AssignmentResponseDto response = assignmentService.findById(id, callerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Restituisce i compiti di un corso", description = "Restituisce tutti i compiti di un corso. TEACHER può vedere solo corsi che insegna. STUDENT può vedere solo corsi in cui è iscritto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista compiti del corso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Accesso negato (non insegni in questo corso o non sei iscritto)"),
            @ApiResponse(responseCode = "404", description = "Corso non trovato")
    })
    public ResponseEntity<List<AssignmentResponseDto>> getAssignmentsByCourse(@PathVariable String courseId) {

        String callerId = SecurityUtils.getCurrentUserId();
        List<AssignmentResponseDto> response = assignmentService.getAssignmentsByCourse(courseId, callerId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Modifica un compito", description = "Permette a un docente di modificare un proprio compito.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compito modificato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dati non validi (validazione fallita)"),
            @ApiResponse(responseCode = "403", description = "Accesso negato (non sei il proprietario)"),
            @ApiResponse(responseCode = "404", description = "Compito non trovato")
    })
    public ResponseEntity<AssignmentResponseDto> updateAssignment(
            @PathVariable String id,
            @Valid @RequestBody AssignmentUpdateDto request) {

        String teacherId = SecurityUtils.getCurrentUserId();
        AssignmentResponseDto response = assignmentService.updateAssignment(id, request, teacherId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Elimina un compito", description = "Permette a un docente di eliminare un proprio compito. Di conseguenza verranno eliminate anche tutte le consegne e i file associati.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Compito eliminato con successo"),
            @ApiResponse(responseCode = "403", description = "Accesso negato (non sei il proprietario)"),
            @ApiResponse(responseCode = "404", description = "Compito non trovato")
    })
    public ResponseEntity<Void> deleteAssignment(@PathVariable String id) {
        String teacherId = SecurityUtils.getCurrentUserId();
        assignmentService.deleteAssignmentById(id, teacherId);
        return ResponseEntity.noContent().build();
    }
}
