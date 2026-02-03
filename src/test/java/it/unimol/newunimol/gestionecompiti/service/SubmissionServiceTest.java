package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.client.GestioneIscrizioniClient;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.SubmissionStatsDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.SubmissionConverter;
import it.unimol.newunimol.gestionecompiti.messaging.publisher.EventPublisher;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import it.unimol.newunimol.gestionecompiti.model.Submission;
import it.unimol.newunimol.gestionecompiti.model.SubmissionStatus;
import it.unimol.newunimol.gestionecompiti.repository.AssignmentRepository;
import it.unimol.newunimol.gestionecompiti.repository.SubmissionRepository;
import it.unimol.newunimol.gestionecompiti.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per SubmissionService
 * Utilizza Mockito per simulare le dipendenze
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService Tests")
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionConverter submissionConverter;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GestioneIscrizioniClient iscrizioniClient;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SubmissionService submissionService;

    private SubmissionRequestDto validRequest;
    private Submission mockSubmission;
    private SubmissionResponseDto mockResponse;
    private Assignment mockAssignment;
    private String studentId;
    private String teacherId;
    private String assignmentId;
    private String courseId;

    @BeforeEach
    void setUp() {
        // Dati di test comuni
        studentId = "student-123";
        teacherId = "teacher-456";
        assignmentId = "assignment-789";
        courseId = "course-001";

        validRequest = new SubmissionRequestDto("Test submission content");

        mockAssignment = new Assignment();
        mockAssignment.setId(assignmentId);
        mockAssignment.setTitle("Test Assignment");
        mockAssignment.setCourseId(courseId);
        mockAssignment.setTeacherId(teacherId);
        mockAssignment.setDueDate(LocalDateTime.now().plusDays(7));

        mockSubmission = new Submission();
        mockSubmission.setId("submission-001");
        mockSubmission.setAssignmentId(assignmentId);
        mockSubmission.setStudentId(studentId);
        mockSubmission.setContent("Test submission content");
        mockSubmission.setSubmittedAt(LocalDateTime.now());
        mockSubmission.setStatus(SubmissionStatus.SUBMITTED);

        mockResponse = new SubmissionResponseDto(
                "submission-001",
                assignmentId,
                studentId,
                "Test submission content",
                List.of(),
                LocalDateTime.now(),
                SubmissionStatus.SUBMITTED);
    }

    // ==================== CREATE SUBMISSION ====================

    @Test
    @DisplayName("createSubmission - Successo: crea submission on-time")
    void testCreateSubmission_Success_OnTime() {
        // Arrange
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);
        when(submissionConverter.toDto(mockSubmission)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            SubmissionResponseDto result = submissionService.createSubmission(validRequest, assignmentId, studentId);

            // Assert
            assertNotNull(result);
            assertEquals("submission-001", result.id());
            verify(submissionRepository, times(1)).save(any(Submission.class));
            verify(eventPublisher, times(1)).publishSubmissionCreated(any(), eq(courseId), eq(false));
        }
    }

    @Test
    @DisplayName("createSubmission - Successo: crea submission late")
    void testCreateSubmission_Success_Late() {
        // Arrange
        mockAssignment.setDueDate(LocalDateTime.now().minusDays(1)); // Scadenza passata
        mockSubmission.setStatus(SubmissionStatus.LATE);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);
        when(submissionConverter.toDto(mockSubmission)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            SubmissionResponseDto result = submissionService.createSubmission(validRequest, assignmentId, studentId);

            // Assert
            assertNotNull(result);
            verify(submissionRepository, times(1)).save(any(Submission.class));
            verify(eventPublisher, times(1)).publishSubmissionCreated(any(), eq(courseId), eq(true));
        }
    }

    @Test
    @DisplayName("createSubmission - Errore: assignment non trovato")
    void testCreateSubmission_AssignmentNotFound() {
        // Arrange
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> submissionService.createSubmission(validRequest, assignmentId, studentId));

        assertTrue(exception.getMessage().contains("Compito non trovato"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSubmission - Errore: studente non iscritto")
    void testCreateSubmission_StudentNotEnrolled() {
        // Arrange
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> submissionService.createSubmission(validRequest, assignmentId, studentId));

            assertTrue(exception.getMessage().contains("Non puoi consegnare questo compito"));
            verify(submissionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("createSubmission - Errore: già consegnato")
    void testCreateSubmission_AlreadySubmitted() {
        // Arrange
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> submissionService.createSubmission(validRequest, assignmentId, studentId));

            assertEquals("Hai già consegnato questo compito", exception.getMessage());
            verify(submissionRepository, never()).save(any());
        }
    }

    // ==================== GET SUBMISSIONS BY ASSIGNMENT ====================

    @Test
    @DisplayName("getSubmissionsByAssignment - Successo: docente proprietario")
    void testGetSubmissionsByAssignment_Success() {
        // Arrange
        Submission submission2 = new Submission();
        submission2.setId("submission-002");
        List<Submission> submissions = Arrays.asList(mockSubmission, submission2);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.findByAssignmentId(assignmentId)).thenReturn(submissions);
        when(submissionConverter.toDto(any(Submission.class))).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

            // Act
            List<SubmissionResponseDto> result = submissionService.getSubmissionsByAssignment(assignmentId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(submissionRepository, times(1)).findByAssignmentId(assignmentId);
        }
    }

    @Test
    @DisplayName("getSubmissionsByAssignment - Errore: docente non proprietario")
    void testGetSubmissionsByAssignment_NotOwner() {
        // Arrange
        String otherTeacherId = "other-teacher";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(otherTeacherId);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> submissionService.getSubmissionsByAssignment(assignmentId));

            assertTrue(exception.getMessage().contains("Non puoi accedere alle consegne"));
            verify(submissionRepository, never()).findByAssignmentId(anyString());
        }
    }

    // ==================== GET SUBMISSION STATS ====================

    @Test
    @DisplayName("getSubmissionStats - Successo: calcola statistiche correttamente")
    void testGetSubmissionStats_Success() {
        // Arrange
        List<String> enrolledStudents = Arrays.asList("student-1", "student-2", "student-3");
        List<Submission> submissions = Arrays.asList(mockSubmission);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.countByAssignmentId(assignmentId)).thenReturn(1);
        when(submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.SUBMITTED)).thenReturn(1);
        when(submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.LATE)).thenReturn(0);
        when(submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.GRADED)).thenReturn(0);
        when(submissionRepository.countByAssignmentIdAndStatus(assignmentId, SubmissionStatus.PENDING)).thenReturn(0);
        when(submissionRepository.findByAssignmentId(assignmentId)).thenReturn(submissions);
        when(iscrizioniClient.getEnrolledStudentIds(courseId)).thenReturn(enrolledStudents);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

            // Act
            SubmissionStatsDto result = submissionService.getSubmissionStats(assignmentId);

            // Assert
            assertNotNull(result);
            assertEquals(assignmentId, result.assignmentId());
            assertEquals(1, result.totalSubmissions());
            assertEquals(1, result.onTimeSubmissions());
            assertEquals(0, result.lateSubmissions());
            assertEquals(3, result.totalEnrolledStudents());
            assertEquals(2, result.studentsMissing());
            assertEquals(33.33, result.completionRate(), 0.01);
        }
    }

    // ==================== GET STUDENT SUBMISSION ====================

    @Test
    @DisplayName("getStudentSubmission - Successo: docente proprietario")
    void testGetStudentSubmission_Success() {
        // Arrange
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionConverter.toDto(mockSubmission)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            SubmissionResponseDto result = submissionService.getStudentSubmission(assignmentId, studentId, teacherId);

            // Assert
            assertNotNull(result);
            assertEquals("submission-001", result.id());
            verify(submissionRepository, times(1)).findByAssignmentIdAndStudentId(assignmentId, studentId);
        }
    }

    @Test
    @DisplayName("getStudentSubmission - Errore: submission non trovata")
    void testGetStudentSubmission_NotFound() {
        // Arrange
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> submissionService.getStudentSubmission(assignmentId, studentId, teacherId));

        assertTrue(exception.getMessage().contains("Consegna non trovata"));
    }

    // ==================== GET SUBMISSION BY ID ====================

    @Test
    @DisplayName("getSubmissionById - Successo: studente accede alla propria submission")
    void testGetSubmissionById_Student_OwnSubmission() {
        // Arrange
        String submissionId = "submission-001";
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(submissionConverter.toDto(mockSubmission)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(() -> SecurityUtils.hasRole("ROLE_STUDENT")).thenReturn(true);

            // Act
            SubmissionResponseDto result = submissionService.getSubmissionById(submissionId, studentId);

            // Assert
            assertNotNull(result);
            assertEquals("submission-001", result.id());
        }
    }

    @Test
    @DisplayName("getSubmissionById - Errore: studente accede a submission altrui")
    void testGetSubmissionById_Student_OtherSubmission() {
        // Arrange
        String submissionId = "submission-001";
        String otherStudentId = "other-student";

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurity.when(() -> SecurityUtils.hasRole("ROLE_STUDENT")).thenReturn(true);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> submissionService.getSubmissionById(submissionId, otherStudentId));

            assertTrue(exception.getMessage().contains("Puoi vedere solo le tue consegne"));
        }
    }

    // ==================== UPDATE SUBMISSION ====================

    @Test
    @DisplayName("updateSubmission - Successo: modifica submission")
    void testUpdateSubmission_Success() {
        // Arrange
        String submissionId = "submission-001";
        SubmissionRequestDto updateRequest = new SubmissionRequestDto("Updated content");

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);
        when(submissionConverter.toDto(mockSubmission)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            SubmissionResponseDto result = submissionService.updateSubmission(submissionId, updateRequest, studentId);

            // Assert
            assertNotNull(result);
            verify(submissionRepository, times(1)).save(mockSubmission);
        }
    }

    @Test
    @DisplayName("updateSubmission - Errore: submission già valutata")
    void testUpdateSubmission_AlreadyGraded() {
        // Arrange
        String submissionId = "submission-001";
        mockSubmission.setStatus(SubmissionStatus.GRADED);
        SubmissionRequestDto updateRequest = new SubmissionRequestDto("Updated content");

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> submissionService.updateSubmission(submissionId, updateRequest, studentId));

            assertTrue(exception.getMessage().contains("Non puoi modificare una consegna già valutata"));
            verify(submissionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("updateSubmission - Errore: scadenza passata")
    void testUpdateSubmission_AfterDeadline() {
        // Arrange
        String submissionId = "submission-001";
        mockAssignment.setDueDate(LocalDateTime.now().minusDays(1));
        SubmissionRequestDto updateRequest = new SubmissionRequestDto("Updated content");

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> submissionService.updateSubmission(submissionId, updateRequest, studentId));

            assertTrue(exception.getMessage().contains("Non è possibile modificare una consegna dopo la scadenza"));
            verify(submissionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("updateSubmission - Errore: studente non proprietario")
    void testUpdateSubmission_NotOwner() {
        // Arrange
        String submissionId = "submission-001";
        String otherStudentId = "other-student";
        SubmissionRequestDto updateRequest = new SubmissionRequestDto("Updated content");

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> submissionService.updateSubmission(submissionId, updateRequest, otherStudentId));

            assertTrue(exception.getMessage().contains("Non hai i permessi per modificare"));
            verify(submissionRepository, never()).save(any());
        }
    }

    // ==================== DELETE SUBMISSION ====================

    @Test
    @DisplayName("deleteSubmission - Successo: elimina submission")
    void testDeleteSubmission_Success() {
        // Arrange
        String submissionId = "submission-001";

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            submissionService.deleteSubmission(submissionId, studentId);

            // Assert
            verify(fileStorageService, times(1)).deleteAllFilesByEntity(
                    eq(FileEntityType.SUBMISSION),
                    eq(submissionId));
            verify(submissionRepository, times(1)).deleteById(submissionId);
        }
    }

    @Test
    @DisplayName("deleteSubmission - Errore: submission già valutata")
    void testDeleteSubmission_AlreadyGraded() {
        // Arrange
        String submissionId = "submission-001";
        mockSubmission.setStatus(SubmissionStatus.GRADED);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> submissionService.deleteSubmission(submissionId, studentId));

            assertTrue(exception.getMessage().contains("Non puoi eliminare una consegna già valutata"));
            verify(submissionRepository, never()).deleteById(anyString());
        }
    }

    @Test
    @DisplayName("deleteSubmission - Errore: scadenza passata")
    void testDeleteSubmission_AfterDeadline() {
        // Arrange
        String submissionId = "submission-001";
        mockAssignment.setDueDate(LocalDateTime.now().minusDays(1));

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> submissionService.deleteSubmission(submissionId, studentId));

            assertTrue(exception.getMessage().contains("Non puoi eliminare una consegna dopo la scadenza"));
            verify(submissionRepository, never()).deleteById(anyString());
        }
    }

    @Test
    @DisplayName("deleteSubmission - Errore: studente non proprietario")
    void testDeleteSubmission_NotOwner() {
        // Arrange
        String submissionId = "submission-001";
        String otherStudentId = "other-student";

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(mockSubmission));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> submissionService.deleteSubmission(submissionId, otherStudentId));

            assertTrue(exception.getMessage().contains("Non hai i permessi per eliminare"));
            verify(submissionRepository, never()).deleteById(anyString());
        }
    }

    // ==================== COUNT SUBMISSIONS ====================

    @Test
    @DisplayName("countSubmissionsByAssignment - Successo: conta submissions")
    void testCountSubmissionsByAssignment() {
        // Arrange
        when(submissionRepository.countByAssignmentId(assignmentId)).thenReturn(5);

        // Act
        int count = submissionService.countSubmissionsByAssignment(assignmentId);

        // Assert
        assertEquals(5, count);
        verify(submissionRepository, times(1)).countByAssignmentId(assignmentId);
    }
}
