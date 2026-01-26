package it.unimol.newunimol.gestionecompiti.controller;

import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.service.SubmissionService;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionStatsDto;
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
@RequestMapping("/api/v1/submissions")
@Tag(name = "Submission", description = "API per la gestione delle consegne")
public class SubmissionController {

        @Autowired
        private SubmissionService submissionService;

        @PostMapping("/assignment/{assignmentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
        @Operation(summary = "Crea una nuova consegna", description = "Permette a uno studente di consegnare un compito. Per aggiungere file usa POST /api/v1/files/submission/{id}/upload")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Consegna creata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Dati non validi o consegna già esistente"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (solo studenti)"),
                        @ApiResponse(responseCode = "404", description = "Compito non trovato")
        })
        public ResponseEntity<SubmissionResponseDto> createSubmission(
                        @PathVariable String assignmentId,
                        @Valid @RequestBody SubmissionRequestDto request) {
                String studentId = SecurityUtils.getCurrentUserId();
                SubmissionResponseDto response = submissionService.createSubmission(request, assignmentId, studentId);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/assignment/{assignmentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        @Operation(summary = "Ottiene le consegne relative ad un compito", description = "Restituisce tutte le consegne per un compito specifico, a patto che il docente sia il proprietario del compito")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista consegne del compito", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (solo docenti proprietari)"),
                        @ApiResponse(responseCode = "404", description = "Compito non trovato")
        })
        public ResponseEntity<List<SubmissionResponseDto>> getSubmissionsByAssignment(
                        @PathVariable String assignmentId) {
                List<SubmissionResponseDto> response = submissionService.getSubmissionsByAssignment(assignmentId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/assignment/{assignmentId}/stats")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        @Operation(summary = "Statistiche consegne", description = "Restituisce statistiche aggregate sulle consegne di un compito: totali, in tempo (SUBMITTED), in ritardo (LATE), già valutate (GRADED), in attesa (PENDING). Solo il docente proprietario può accedere.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statistiche calcolate con successo.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionStatsDto.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (solo docenti proprietari)"),
                        @ApiResponse(responseCode = "404", description = "Compito non trovato")
        })
        public ResponseEntity<SubmissionStatsDto> getSubmissionStats(@PathVariable String assignmentId) {
                SubmissionStatsDto stats = submissionService.getSubmissionStats(assignmentId);
                return ResponseEntity.ok(stats);
        }

        @GetMapping("/assignment/{assignmentId}/student/{studentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        @Operation(summary = "Ottiene la consegna per un assignment da parte di uno studente", description = "Permette al docente di visualizzare la consegna di uno studente specifico per un proprio assignment. Gli studenti devono usare GET /{submissionId} per vedere le proprie consegne.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Consegna trovata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (studente può vedere solo le proprie, docente solo dei propri assignment)"),
                        @ApiResponse(responseCode = "404", description = "Consegna non trovata")
        })
        public ResponseEntity<SubmissionResponseDto> getStudentSubmission(
                        @PathVariable String assignmentId,
                        @PathVariable String studentId) {
                String callerId = SecurityUtils.getCurrentUserId();
                SubmissionResponseDto response = submissionService.getStudentSubmission(assignmentId, studentId,
                                callerId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{submissionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        @Operation(summary = "Ottiene una consegna per ID", description = "Restituisce i dettagli di una consegna specifica. Studenti vedono solo le proprie, docenti solo dei propri assignment.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Consegna trovata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (studente può vedere solo le proprie, docente solo dei propri assignment)"),
                        @ApiResponse(responseCode = "404", description = "Consegna non trovata")
        })
        public ResponseEntity<SubmissionResponseDto> getSubmissionById(@PathVariable String submissionId) {
                String callerId = SecurityUtils.getCurrentUserId();
                SubmissionResponseDto response = submissionService.getSubmissionById(submissionId, callerId);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{submissionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
        @Operation(summary = "Modifica una consegna", description = "Permette a uno studente di modificare il contenuto della propria consegna prima della scadenza. La data di consegna originale (submittedAt) viene mantenuta per auditing. I file allegati devono essere gestiti tramite FileController.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Consegna modificata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Dati non validi o scadenza superata"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (non sei il proprietario)"),
                        @ApiResponse(responseCode = "404", description = "Consegna non trovata")
        })
        public ResponseEntity<SubmissionResponseDto> updateSubmission(
                        @PathVariable String submissionId,
                        @Valid @RequestBody SubmissionRequestDto request) {
                String studentId = SecurityUtils.getCurrentUserId();
                SubmissionResponseDto response = submissionService.updateSubmission(submissionId, request, studentId);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{submissionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
        @Operation(summary = "Elimina una consegna", description = "Permette di eliminare una propria consegna SOLO se: 1) Non è ancora stata valutata, 2) La scadenza del compito non è ancora passata. Elimina in cascata anche i file allegati. ADMIN può eliminare qualsiasi consegna.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Consegna eliminata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Eliminazione non consentita: consegna già valutata o scadenza superata"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato (non sei il proprietario)"),
                        @ApiResponse(responseCode = "404", description = "Consegna non trovata")
        })
        public ResponseEntity<Void> deleteSubmission(@PathVariable String submissionId) {
                String studentId = SecurityUtils.getCurrentUserId();
                submissionService.deleteSubmission(submissionId, studentId);
                return ResponseEntity.noContent().build();
        }
}
