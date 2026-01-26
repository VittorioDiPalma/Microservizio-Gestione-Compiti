package it.unimol.newunimol.gestionecompiti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unimol.newunimol.gestionecompiti.model.FileAttachment;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import it.unimol.newunimol.gestionecompiti.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "API per la gestione degli allegati di Assignment e Submission")
public class FileController {

        @Autowired
        private FileStorageService fileStorageService;

        @PostMapping(value = "/assignment/{assignmentId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        @Operation(summary = "Upload file per Assignment", description = "Carica un file allegato a un Assignment. Solo il docente proprietario può caricare file.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File caricato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileAttachment.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato"),
                        @ApiResponse(responseCode = "400", description = "File non valido")
        })
        public ResponseEntity<FileAttachment> uploadAssignmentFile(
                        @PathVariable String assignmentId,
                        @RequestParam("file") @io.swagger.v3.oas.annotations.Parameter(description = "File da caricare (PDF, DOCX, PNG, JPG, ZIP, TXT - max 10MB)", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) MultipartFile file) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                FileAttachment fileAttachment = fileStorageService.storeAssignmentFile(
                                file, assignmentId, currentUserId);
                return ResponseEntity.ok(fileAttachment);
        }

        @PostMapping(value = "/submission/{submissionId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
        @Operation(summary = "Upload file per Submission", description = "Carica un file allegato a una Submission. Solo lo studente proprietario può caricare file.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File caricato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileAttachment.class))),
                        @ApiResponse(responseCode = "403", description = "Accesso negato"),
                        @ApiResponse(responseCode = "400", description = "File non valido")
        })
        public ResponseEntity<FileAttachment> uploadSubmissionFile(
                        @PathVariable String submissionId,
                        @RequestParam("file") @io.swagger.v3.oas.annotations.Parameter(description = "File da caricare (PDF, DOCX, PNG, JPG, ZIP, TXT - max 10MB)", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) MultipartFile file) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                FileAttachment fileAttachment = fileStorageService.storeSubmissionFile(
                                file, submissionId, currentUserId);
                return ResponseEntity.ok(fileAttachment);
        }

        @GetMapping("/download/{fileId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        @Operation(summary = "Download file", description = "Scarica un file allegato, se autorizzato. TEACHER e STUDENT possono scaricare solo i file di cui sono proprietari o relativi ai loro corsi.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File scaricato con successo"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato"),
                        @ApiResponse(responseCode = "404", description = "File non trovato")
        })
        public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                FileAttachment fileMetadata = fileStorageService.getFileMetadata(fileId);

                fileStorageService.verifyFileAccess(fileMetadata, currentUserId);

                Resource resource = fileStorageService.loadFileAsResource(fileId);

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(fileMetadata.getMimeType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + fileMetadata.getOriginalFilename() + "\"")
                                .body(resource);
        }

        @GetMapping("/assignment/{assignmentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        @Operation(summary = "Lista file per Assignment", description = "Ottiene la lista dei file allegati a un Assignment. Solo utenti autorizzati possono accedere.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista file ottenuta con successo"),
                        @ApiResponse(responseCode = "404", description = "Assignment non trovato")
        })
        public ResponseEntity<List<FileAttachment>> getAssignmentFiles(@PathVariable String assignmentId) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                List<FileAttachment> files = fileStorageService.getAssignmentFiles(assignmentId, currentUserId);
                return ResponseEntity.ok(files);
        }

        @GetMapping("/submission/{submissionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        @Operation(summary = "Lista file per Submission", description = "Ottiene la lista dei file allegati a una Submission. Solo utenti autorizzati possono accedere.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista file ottenuta con successo"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato"),
                        @ApiResponse(responseCode = "404", description = "Submission non trovata")
        })
        public ResponseEntity<List<FileAttachment>> getSubmissionFiles(@PathVariable String submissionId) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                List<FileAttachment> files = fileStorageService.getSubmissionFiles(submissionId, currentUserId);
                return ResponseEntity.ok(files);
        }

        @DeleteMapping("/{fileId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        @Operation(summary = "Elimina file", description = "Elimina un file allegato. Solo il proprietario del file può eliminarlo.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File eliminato con successo"),
                        @ApiResponse(responseCode = "403", description = "Accesso negato"),
                        @ApiResponse(responseCode = "404", description = "File non trovato")
        })
        public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
                String currentUserId = SecurityUtils.getCurrentUserId();
                fileStorageService.deleteFileWithAuthorization(fileId, currentUserId);
                return ResponseEntity.ok().build();
        }
}
