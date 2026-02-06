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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private GestioneIscrizioniClient iscrizioniClient;

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile mockFile;

    private FileAttachment mockFileAttachment;
    private Assignment mockAssignment;
    private Submission mockSubmission;
    private String userId;
    private String teacherId;
    private String studentId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        teacherId = "teacher-456";
        studentId = "student-789";

        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());

        mockFileAttachment = new FileAttachment();
        mockFileAttachment.setId("file-001");
        mockFileAttachment.setOriginalFilename("test.pdf");
        mockFileAttachment.setStoredFilename("uuid-test.pdf");
        mockFileAttachment.setFilepath("assignments/uuid-test.pdf");
        mockFileAttachment.setMimeType("application/pdf");
        mockFileAttachment.setFileSize(1024L);
        mockFileAttachment.setUploadedBy(userId);
        mockFileAttachment.setEntityType(FileEntityType.ASSIGNMENT);
        mockFileAttachment.setEntityId("assignment-001");

        mockAssignment = new Assignment();
        mockAssignment.setId("assignment-001");
        mockAssignment.setTeacherId(teacherId);
        mockAssignment.setCourseId("course-001");

        mockSubmission = new Submission();
        mockSubmission.setId("submission-001");
        mockSubmission.setStudentId(studentId);
        mockSubmission.setAssignmentId("assignment-001");
    }

    @Test
    @DisplayName("getFileMetadata - Successo: ottiene metadata del file")
    void testGetFileMetadata_Success() {
        String fileId = "file-001";
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.of(mockFileAttachment));

        FileAttachment result = fileStorageService.getFileMetadata(fileId);

        assertNotNull(result);
        assertEquals("file-001", result.getId());
        assertEquals("test.pdf", result.getOriginalFilename());
        verify(fileAttachmentRepository, times(1)).findById(fileId);
    }

    @Test
    @DisplayName("getFileMetadata - Errore: file non trovato")
    void testGetFileMetadata_NotFound() {
        String fileId = "non-existent";
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> fileStorageService.getFileMetadata(fileId));

        assertTrue(exception.getMessage().contains("File non trovato"));
    }

    @Test
    @DisplayName("getFilesByEntity - Successo: ottiene lista file")
    void testGetFilesByEntity_Success() {
        String entityId = "assignment-001";
        FileAttachment file2 = new FileAttachment();
        file2.setId("file-002");
        List<FileAttachment> files = Arrays.asList(mockFileAttachment, file2);

        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, entityId))
                .thenReturn(files);

        List<FileAttachment> result = fileStorageService.getFilesByEntity(FileEntityType.ASSIGNMENT, entityId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(fileAttachmentRepository, times(1))
                .findByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, entityId);
    }

    @Test
    @DisplayName("getAssignmentFiles - Successo: docente proprietario")
    void testGetAssignmentFiles_Teacher_Success() {
        String assignmentId = "assignment-001";
        List<FileAttachment> files = Arrays.asList(mockFileAttachment);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, assignmentId))
                .thenReturn(files);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            List<FileAttachment> result = fileStorageService.getAssignmentFiles(assignmentId, teacherId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("getAssignmentFiles - Successo: studente iscritto")
    void testGetAssignmentFiles_EnrolledStudent_Success() {
        String assignmentId = "assignment-001";
        List<FileAttachment> files = Arrays.asList(mockFileAttachment);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, "course-001")).thenReturn(true);
        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, assignmentId))
                .thenReturn(files);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            List<FileAttachment> result = fileStorageService.getAssignmentFiles(assignmentId, studentId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("getAssignmentFiles - Errore: utente non autorizzato")
    void testGetAssignmentFiles_Unauthorized() {
        String assignmentId = "assignment-001";
        String unauthorizedUserId = "unauthorized-user";

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(unauthorizedUserId, "course-001")).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> fileStorageService.getAssignmentFiles(assignmentId, unauthorizedUserId));

            assertTrue(exception.getMessage().contains("Non puoi visualizzare i file"));
        }
    }

    @Test
    @DisplayName("getAssignmentFiles - Errore: assignment non trovato")
    void testGetAssignmentFiles_AssignmentNotFound() {
        String assignmentId = "non-existent";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> fileStorageService.getAssignmentFiles(assignmentId, userId));

        assertTrue(exception.getMessage().contains("Assignment non trovato"));
    }

    @Test
    @DisplayName("getSubmissionFiles - Successo: studente proprietario")
    void testGetSubmissionFiles_Owner_Success() {
        String submissionId = "submission-001";
        List<FileAttachment> files = Arrays.asList(mockFileAttachment);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.SUBMISSION, submissionId))
                .thenReturn(files);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            List<FileAttachment> result = fileStorageService.getSubmissionFiles(submissionId, studentId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("getSubmissionFiles - Successo: docente dell'assignment")
    void testGetSubmissionFiles_Teacher_Success() {
        String submissionId = "submission-001";
        List<FileAttachment> files = Arrays.asList(mockFileAttachment);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));
        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.SUBMISSION, submissionId))
                .thenReturn(files);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::isTeacher).thenReturn(true);

            List<FileAttachment> result = fileStorageService.getSubmissionFiles(submissionId, teacherId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("getSubmissionFiles - Errore: utente non autorizzato")
    void testGetSubmissionFiles_Unauthorized() {
        String submissionId = "submission-001";
        String unauthorizedUserId = "unauthorized-user";

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::isTeacher).thenReturn(false);

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> fileStorageService.getSubmissionFiles(submissionId, unauthorizedUserId));

            assertTrue(exception.getMessage().contains("Non puoi visualizzare i file"));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Successo: admin bypassa controlli")
    void testVerifyFileAccess_Admin() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);

            assertDoesNotThrow(() -> fileStorageService.verifyFileAccess(mockFileAttachment, "any-user"));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Successo: docente proprietario assignment")
    void testVerifyFileAccess_TeacherOwner() {
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            assertDoesNotThrow(() -> fileStorageService.verifyFileAccess(mockFileAttachment, teacherId));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Successo: studente iscritto al corso")
    void testVerifyFileAccess_EnrolledStudent() {
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, "course-001")).thenReturn(true);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            assertDoesNotThrow(() -> fileStorageService.verifyFileAccess(mockFileAttachment, studentId));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Errore: studente non iscritto")
    void testVerifyFileAccess_NotEnrolled() {
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, "course-001")).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> fileStorageService.verifyFileAccess(mockFileAttachment, studentId));

            assertTrue(exception.getMessage().contains("Non puoi accedere a questo file"));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Successo: submission - studente proprietario")
    void testVerifyFileAccess_SubmissionOwner() {
        mockFileAttachment.setEntityType(FileEntityType.SUBMISSION);
        mockFileAttachment.setEntityId("submission-001");

        when(submissionRepository.findById("submission-001")).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            assertDoesNotThrow(() -> fileStorageService.verifyFileAccess(mockFileAttachment, studentId));
        }
    }

    @Test
    @DisplayName("verifyFileAccess - Successo: submission - docente dell'assignment")
    void testVerifyFileAccess_SubmissionTeacher() {
        mockFileAttachment.setEntityType(FileEntityType.SUBMISSION);
        mockFileAttachment.setEntityId("submission-001");

        when(submissionRepository.findById("submission-001")).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::isTeacher).thenReturn(true);

            assertDoesNotThrow(() -> fileStorageService.verifyFileAccess(mockFileAttachment, teacherId));
        }
    }

    @Test
    @DisplayName("deleteAllFilesByEntity - Successo: elimina tutti i file")
    void testDeleteAllFilesByEntity_Success() {
        String entityId = "assignment-001";
        FileAttachment file2 = new FileAttachment();
        file2.setFilepath("assignments/uuid-test2.pdf");
        List<FileAttachment> files = Arrays.asList(mockFileAttachment, file2);

        when(fileAttachmentRepository.findByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, entityId))
                .thenReturn(files);

        fileStorageService.deleteAllFilesByEntity(FileEntityType.ASSIGNMENT, entityId);

        verify(fileAttachmentRepository, times(1))
                .deleteByEntityTypeAndEntityId(FileEntityType.ASSIGNMENT, entityId);
    }

    @Test
    @DisplayName("validateFile - Errore: file vuoto")
    void testValidateFile_EmptyFile() {
        when(mockFile.isEmpty()).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(mockFile, FileEntityType.ASSIGNMENT, "assignment-001", userId));

        assertTrue(exception.getMessage().contains("File vuoto"));
    }

    @Test
    @DisplayName("validateFile - Errore: file troppo grande")
    void testValidateFile_FileTooLarge() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(11 * 1024 * 1024L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(mockFile, FileEntityType.ASSIGNMENT, "assignment-001", userId));

        assertTrue(exception.getMessage().contains("troppo grande")
                || exception.getMessage().contains("dimensione massima"));
    }

    @Test
    @DisplayName("validateFile - Errore: MIME type non consentito")
    void testValidateFile_InvalidMimeType() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/exe");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(mockFile, FileEntityType.ASSIGNMENT, "assignment-001", userId));

        assertTrue(
                exception.getMessage().contains("non permesso") || exception.getMessage().contains("non consentito"));
    }

    @Test
    @DisplayName("validateFile - Errore: nome file pericoloso")
    void testValidateFile_DangerousFilename() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("../../../etc/passwd");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(mockFile, FileEntityType.ASSIGNMENT, "assignment-001", userId));

        assertTrue(exception.getMessage().contains("Nome file non valido"));
    }

    @Test
    @DisplayName("validateFile - Successo: file valido")
    void testValidateFile_ValidFile() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");

        assertFalse(mockFile.isEmpty());
        assertTrue(mockFile.getSize() <= 10 * 1024 * 1024);
        assertEquals("application/pdf", mockFile.getContentType());
    }

    @Test
    @DisplayName("deleteFile - Successo: elimina file esistente")
    void testDeleteFile_Success() {
        String fileId = "file-001";
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.of(mockFileAttachment));

        fileStorageService.deleteFile(fileId);

        verify(fileAttachmentRepository, times(1)).deleteById(fileId);
    }

    @Test
    @DisplayName("deleteFile - Errore: file non trovato")
    void testDeleteFile_NotFound() {
        String fileId = "non-existent";
        when(fileAttachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> fileStorageService.deleteFile(fileId));

        assertTrue(exception.getMessage().contains("File non trovato"));
    }

    @Test
    @DisplayName("storeFile - Successo: salva file reale su filesystem")
    void testStoreFile_Success() throws IOException {
        fileStorageService.init();

        byte[] fileContent = "Test PDF content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) fileContent.length);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(i -> {
            FileAttachment fa = i.getArgument(0);
            fa.setId("file-saved-001");
            return fa;
        });

        FileAttachment result = fileStorageService.storeFile(
                mockFile,
                FileEntityType.ASSIGNMENT,
                "assignment-001",
                userId);

        assertNotNull(result);
        assertEquals("document.pdf", result.getOriginalFilename());
        assertEquals("application/pdf", result.getMimeType());
        assertEquals(fileContent.length, result.getFileSize());
        assertEquals(userId, result.getUploadedBy());
        assertEquals(FileEntityType.ASSIGNMENT, result.getEntityType());
        assertEquals("assignment-001", result.getEntityId());

        Path savedFile = tempDir.resolve(result.getFilepath());
        assertTrue(Files.exists(savedFile));

        byte[] savedContent = Files.readAllBytes(savedFile);
        assertArrayEquals(fileContent, savedContent);

        verify(fileAttachmentRepository, times(1)).save(any(FileAttachment.class));
    }

    @Test
    @DisplayName("storeAssignmentFile - Successo: docente salva file per assignment")
    void testStoreAssignmentFile_Success() throws IOException {
        fileStorageService.init();

        byte[] fileContent = "Assignment file".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) fileContent.length);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("assignment.pdf");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(assignmentRepository.findById("assignment-001")).thenReturn(Optional.of(mockAssignment));
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(i -> i.getArgument(0));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            FileAttachment result = fileStorageService.storeAssignmentFile(
                    mockFile,
                    "assignment-001",
                    teacherId);

            assertNotNull(result);
            assertEquals("assignment.pdf", result.getOriginalFilename());
            assertTrue(Files.exists(tempDir.resolve(result.getFilepath())));
        }
    }

    @Test
    @DisplayName("storeSubmissionFile - Successo: studente salva file per submission")
    void testStoreSubmissionFile_Success() throws IOException {
        fileStorageService.init();

        byte[] fileContent = "Submission file".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) fileContent.length);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("submission.pdf");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(submissionRepository.findById("submission-001")).thenReturn(Optional.of(mockSubmission));
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(i -> i.getArgument(0));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            FileAttachment result = fileStorageService.storeSubmissionFile(
                    mockFile,
                    "submission-001",
                    studentId);

            assertNotNull(result);
            assertEquals("submission.pdf", result.getOriginalFilename());
            assertTrue(Files.exists(tempDir.resolve(result.getFilepath())));
        }
    }

    @Test
    @DisplayName("loadFileAsResource - Successo: carica file esistente")
    void testLoadFileAsResource_Success() throws IOException {
        fileStorageService.init();
        Path assignmentsDir = tempDir.resolve("assignments");
        Files.createDirectories(assignmentsDir);
        Path testFile = assignmentsDir.resolve("test-file.pdf");
        Files.write(testFile, "Test content".getBytes());

        mockFileAttachment.setFilepath("assignments/test-file.pdf");
        when(fileAttachmentRepository.findById("file-001")).thenReturn(Optional.of(mockFileAttachment));

        Resource resource = fileStorageService.loadFileAsResource("file-001");

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        assertEquals("test-file.pdf", resource.getFilename());
    }

    @Test
    @DisplayName("loadFileAsResource - Errore: file non trovato su filesystem")
    void testLoadFileAsResource_FileNotFoundOnDisk() throws IOException {
        fileStorageService.init();
        mockFileAttachment.setFilepath("assignments/non-existent.pdf");
        when(fileAttachmentRepository.findById("file-001")).thenReturn(Optional.of(mockFileAttachment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.loadFileAsResource("file-001"));

        assertTrue(exception.getMessage().contains("caricamento") ||
                exception.getMessage().contains("leggibile"));
    }

    @Test
    @DisplayName("deleteFile - Successo: elimina file da filesystem e DB")
    void testDeleteFile_WithRealFile() throws IOException {
        fileStorageService.init();

        Path assignmentsDir = tempDir.resolve("assignments");
        Files.createDirectories(assignmentsDir);
        Path testFile = assignmentsDir.resolve("to-delete.pdf");
        Files.write(testFile, "To be deleted".getBytes());

        mockFileAttachment.setFilepath("assignments/to-delete.pdf");
        when(fileAttachmentRepository.findById("file-001")).thenReturn(Optional.of(mockFileAttachment));

        assertTrue(Files.exists(testFile), "File should exist before deletion");

        fileStorageService.deleteFile("file-001");

        assertFalse(Files.exists(testFile), "File should be deleted from filesystem");
        verify(fileAttachmentRepository, times(1)).deleteById("file-001");
    }
}
