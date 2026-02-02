package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.client.GestioneIscrizioniClient;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.FileAttachment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.FileAttachmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GestioneIscrizioniClient iscrizioniClient;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain");

    /**
     * Crea le cartelle di upload se non esistono
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir, "assignments"));
            Files.createDirectories(Paths.get(uploadDir, "submissions"));
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare le cartelle di upload", e);
        }
    }

    /**
     * Salva un file sul filesystem e registra i metadata nel DB
     * 
     * @param file       file caricato
     * @param entityType tipo di entità a cui il file è associato (ASSIGNMENT o
     *                   SUBMISSION)
     * @param entityId   ID dell'entità a cui il file è associato
     * @param userId     ID dell'utente che carica il file
     * 
     * @return i metadata del file salvato
     */
    public FileAttachment storeFile(
            MultipartFile file,
            FileEntityType entityType,
            String entityId,
            String userId) {
        validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String storedFilename = UUID.randomUUID().toString() + extension;

            // determina la sottocartella in base al tipo di entità
            String subfolder = entityType == FileEntityType.ASSIGNMENT ? "assignments" : "submissions";
            Path targetLocation = Paths.get(uploadDir, subfolder, storedFilename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileAttachment fileAttachment = new FileAttachment();
            fileAttachment.setOriginalFilename(originalFilename);
            fileAttachment.setStoredFilename(storedFilename);
            fileAttachment.setFilepath(subfolder + "/" + storedFilename);
            fileAttachment.setMimeType(file.getContentType());
            fileAttachment.setFileSize(file.getSize());
            fileAttachment.setUploadedBy(userId);
            fileAttachment.setEntityType(entityType);
            fileAttachment.setEntityId(entityId);

            fileAttachment = fileAttachmentRepository.save(fileAttachment);

            return fileAttachment;

        } catch (IOException e) {
            throw new RuntimeException("Errore nel salvataggio del file: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Carica un file come risorsa per il download
     * 
     * @param fileId ID del file da caricare
     * 
     * @return risorsa del file pronta per il download
     */
    public Resource loadFileAsResource(String fileId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File non trovato con ID: " + fileId));

        try {
            Path filePath = Paths.get(uploadDir, fileAttachment.getFilepath());
            Resource resource = new UrlResource(filePath.toUri());

            // se il file venisse cancellato manualmente dal filesystem
            // risulterebbe comunque nel database, quindi ne verifico l'effettiva esistenza
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File non leggibile: " + fileAttachment.getOriginalFilename());
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento del file", e);
        }
    }

    public FileAttachment getFileMetadata(String fileId) {
        return fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File non trovato con ID: " + fileId));
    }

    /**
     * Ottiene tutti i file associati a una entità
     * 
     * @param entityType tipo di entità
     * @param entityId   ID dell'entità
     * 
     * @return lista di file associati all'entità
     */
    public List<FileAttachment> getFilesByEntity(FileEntityType entityType, String entityId) {
        return fileAttachmentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Elimina un file (filesystem + DB)
     * 
     * @param fileId ID del file da eliminare
     */
    public void deleteFile(String fileId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File non trovato con ID: " + fileId));

        try {
            // eliminazione file fisico prima del DB
            // se fallisce, il record DB non viene toccato e si può riprovare
            Path filePath = Paths.get(uploadDir, fileAttachment.getFilepath());
            Files.deleteIfExists(filePath);

            // eliminazione record DB
            fileAttachmentRepository.deleteById(fileId);

        } catch (IOException e) {
            throw new RuntimeException("Errore nell'eliminazione del file", e);
        }
    }

    /**
     * Elimina tutti i file associati a una entità
     * 
     * @param entityType tipo di entità
     * @param entityId   ID dell'entità
     */
    public void deleteAllFilesByEntity(FileEntityType entityType, String entityId) {
        List<FileAttachment> files = fileAttachmentRepository.findByEntityTypeAndEntityId(entityType, entityId);

        for (FileAttachment file : files) {
            try {
                Path filePath = Paths.get(uploadDir, file.getFilepath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Errore eliminazione file: " + file.getFilepath());
            }
        }

        fileAttachmentRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Valida il file caricato per tipo, dimensione e nome
     * 
     * @param file file da validare
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File vuoto");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File troppo grande. Dimensione massima: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "Tipo di file non permesso. Tipi consentiti: PDF, DOCX, DOC, PNG, JPG, ZIP, TXT");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Nome file non valido");
        }
    }

    /**
     * Upload file per Assignment con verifica ownership
     * 
     * @param file         file da caricare
     * @param assignmentId ID dell'assignment
     * @param userId       ID dell'utente che carica il file
     * 
     * @return i metadata del file salvato
     */
    public FileAttachment storeAssignmentFile(MultipartFile file, String assignmentId, String userId) {
        verifyEntityOwnership(FileEntityType.ASSIGNMENT, assignmentId, userId);
        return storeFile(file, FileEntityType.ASSIGNMENT, assignmentId, userId);
    }

    /**
     * Upload file per Submission con verifica ownership
     * 
     * @param file         file da caricare
     * @param submissionId ID della submission
     * @param userId       ID dell'utente che carica il file
     * 
     * @return i metadata del file salvato
     */
    public FileAttachment storeSubmissionFile(MultipartFile file, String submissionId, String userId) {
        verifyEntityOwnership(FileEntityType.SUBMISSION, submissionId, userId);
        return storeFile(file, FileEntityType.SUBMISSION, submissionId, userId);
    }

    /**
     * Verifica che l'utente abbia i permessi per caricare file sull'entità
     * 
     * @param entityType tipo di entità
     * @param entityId   ID dell'entità
     * @param userId     ID dell'utente
     */
    private void verifyEntityOwnership(FileEntityType entityType, String entityId, String userId) {
        if (SecurityUtils.isAdmin()) {
            return; // ADMIN può caricare su qualsiasi entità
        }

        if (entityType == FileEntityType.ASSIGNMENT) {
            Assignment assignment = assignmentRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));

            if (!assignment.getTeacherId().equals(userId)) {
                throw new AccessDeniedException("Non puoi caricare file per questo assignment");
            }
        } else if (entityType == FileEntityType.SUBMISSION) {
            Submission submission = submissionRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Submission non trovata"));

            if (!submission.getStudentId().equals(userId)) {
                throw new AccessDeniedException("Non puoi caricare file per questa submission");
            }
        }
    }

    /**
     * Ottiene file di un Assignment con verifica permessi
     * 
     * @param assignmentId  ID dell'assignment
     * @param currentUserId ID dell'utente che richiede i file
     * 
     * @return lista di file associati all'assignment
     */
    public List<FileAttachment> getAssignmentFiles(String assignmentId, String currentUserId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));

        if (!SecurityUtils.isAdmin()) {
            boolean isTeacher = assignment.getTeacherId().equals(currentUserId);
            boolean isEnrolled = iscrizioniClient.isStudentEnrolled(currentUserId, assignment.getCourseId());

            if (!isTeacher && !isEnrolled) {
                throw new AccessDeniedException(
                        "Non puoi visualizzare i file di questo assignment. " +
                                "Devi essere il docente proprietario o uno studente iscritto al corso.");
            }
        }

        return getFilesByEntity(FileEntityType.ASSIGNMENT, assignmentId);
    }

    /**
     * Ottiene file di una Submission con verifica permessi
     * 
     * @param submissionId  ID della submission
     * @param currentUserId ID dell'utente che richiede i file
     * 
     * @return lista di file associati alla submission
     */
    public List<FileAttachment> getSubmissionFiles(String submissionId, String currentUserId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission non trovata"));

        if (!SecurityUtils.isAdmin()) {
            boolean isOwner = submission.getStudentId().equals(currentUserId);
            boolean isTeacher = false;

            if (SecurityUtils.isTeacher()) {
                Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                        .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));
                isTeacher = assignment.getTeacherId().equals(currentUserId);
            }

            if (!isOwner && !isTeacher) {
                throw new AccessDeniedException("Non puoi visualizzare i file di questa submission");
            }
        }

        return getFilesByEntity(FileEntityType.SUBMISSION, submissionId);
    }

    /**
     * Verifica che l'utente possa accedere al file
     * 
     * @param fileMetadata  metadata del file
     * @param currentUserId ID dell'utente che richiede il file
     */
    public void verifyFileAccess(FileAttachment fileMetadata, String currentUserId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }

        if (fileMetadata.getEntityType() == FileEntityType.ASSIGNMENT) {
            Assignment assignment = assignmentRepository.findById(fileMetadata.getEntityId())
                    .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));

            if (assignment.getTeacherId().equals(currentUserId)) {
                return;
            }

            if (!iscrizioniClient.isStudentEnrolled(currentUserId, assignment.getCourseId())) {
                throw new AccessDeniedException(
                        "Non puoi accedere a questo file. " +
                                "Devi essere iscritto al corso: " + assignment.getCourseId());
            }
            return;
        }

        if (fileMetadata.getEntityType() == FileEntityType.SUBMISSION) {
            Submission submission = submissionRepository.findById(fileMetadata.getEntityId())
                    .orElseThrow(() -> new EntityNotFoundException("Submission non trovata"));

            boolean isOwner = submission.getStudentId().equals(currentUserId);

            if (isOwner) {
                return;
            }

            if (SecurityUtils.isTeacher()) {
                Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                        .orElseThrow(() -> new EntityNotFoundException("Assignment non trovato"));

                if (assignment.getTeacherId().equals(currentUserId)) {
                    return;
                }
            }

            throw new AccessDeniedException("Non puoi accedere a questo file");
        }
    }

    /**
     * Elimina file con verifica ownership
     * 
     * @param fileId        ID del file da eliminare
     * @param currentUserId ID dell'utente che richiede l'eliminazione
     */
    public void deleteFileWithAuthorization(String fileId, String currentUserId) {
        FileAttachment fileMetadata = getFileMetadata(fileId);

        if (!SecurityUtils.isAdmin() && !fileMetadata.getUploadedBy().equals(currentUserId)) {
            throw new AccessDeniedException("Non puoi eliminare questo file");
        }

        deleteFile(fileId);
    }
}
