package it.unimol.newunimol.gestionecompiti.service;

import it.unimol.newunimol.gestionecompiti.client.GestioneCorsiClient;
import it.unimol.newunimol.gestionecompiti.client.GestioneIscrizioniClient;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentRequestDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentResponseDto;
import it.unimol.newunimol.gestionecompiti.dto.AssignmentUpdateDto;
import it.unimol.newunimol.gestionecompiti.dto.converter.AssignmentConverter;
import it.unimol.newunimol.gestionecompiti.messaging.publisher.EventPublisher;
import it.unimol.newunimol.gestionecompiti.model.Assignment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import it.unimol.newunimol.gestionecompiti.model.Submission;
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
 * Test unitari per AssignmentService
 * Utilizza Mockito per simulare le dipendenze
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Tests")
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentConverter assignmentConverter;

    @Mock
    private GestioneCorsiClient corsiClient;

    @Mock
    private GestioneIscrizioniClient iscrizioniClient;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AssignmentService assignmentService;

    private AssignmentRequestDto validRequest;
    private Assignment mockAssignment;
    private AssignmentResponseDto mockResponse;
    private String teacherId;
    private String courseId;

    @BeforeEach
    void setUp() {
        // Dati di test comuni
        teacherId = "teacher-123";
        courseId = "course-456";

        validRequest = new AssignmentRequestDto(
                "Test Assignment",
                "Test Description",
                LocalDateTime.now().plusDays(7),
                courseId);

        mockAssignment = new Assignment();
        mockAssignment.setId("assignment-789");
        mockAssignment.setTitle("Test Assignment");
        mockAssignment.setDescription("Test Description");
        mockAssignment.setDueDate(LocalDateTime.now().plusDays(7));
        mockAssignment.setCourseId(courseId);
        mockAssignment.setTeacherId(teacherId);
        mockAssignment.setCreatedAt(LocalDateTime.now());

        mockResponse = new AssignmentResponseDto(
                "assignment-789",
                "Test Assignment",
                "Test Description",
                LocalDateTime.now().plusDays(7),
                courseId,
                teacherId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0);
    }

    // ==================== CREATE ASSIGNMENT ====================

    @Test
    @DisplayName("createAssignment - Successo: crea assignment correttamente")
    void testCreateAssignment_Success() {
        // Arrange
        when(corsiClient.courseExists(courseId)).thenReturn(true);
        when(corsiClient.isTeacherOfCourse(courseId, teacherId)).thenReturn(true);
        when(assignmentConverter.toEntity(validRequest)).thenReturn(mockAssignment);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(mockAssignment);
        when(assignmentConverter.toDto(mockAssignment, 0)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            AssignmentResponseDto result = assignmentService.createAssignment(validRequest, teacherId);

            // Assert
            assertNotNull(result);
            assertEquals("assignment-789", result.id());
            assertEquals("Test Assignment", result.title());
            verify(assignmentRepository, times(1)).save(any(Assignment.class));
            verify(eventPublisher, times(1)).publishAssignmentCreated(mockAssignment);
        }
    }

    @Test
    @DisplayName("createAssignment - Errore: data di scadenza passata")
    void testCreateAssignment_PastDueDate() {
        // Arrange
        AssignmentRequestDto pastDateRequest = new AssignmentRequestDto(
                "Test Assignment",
                "Test Description",
                LocalDateTime.now().minusDays(1),
                courseId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.createAssignment(pastDateRequest, teacherId));

        assertEquals("La data di scadenza deve essere futura", exception.getMessage());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAssignment - Errore: corso non trovato")
    void testCreateAssignment_CourseNotFound() {
        // Arrange
        when(corsiClient.courseExists(courseId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.createAssignment(validRequest, teacherId));

        assertTrue(exception.getMessage().contains("Corso non trovato"));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAssignment - Errore: docente non autorizzato")
    void testCreateAssignment_TeacherNotAuthorized() {
        // Arrange
        when(corsiClient.courseExists(courseId)).thenReturn(true);
        when(corsiClient.isTeacherOfCourse(courseId, teacherId)).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> assignmentService.createAssignment(validRequest, teacherId));

            assertTrue(exception.getMessage().contains("Non puoi creare compiti per questo corso"));
            verify(assignmentRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("createAssignment - Successo: admin bypassa controlli docente")
    void testCreateAssignment_AdminBypass() {
        // Arrange
        when(corsiClient.courseExists(courseId)).thenReturn(true);
        when(assignmentConverter.toEntity(validRequest)).thenReturn(mockAssignment);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(mockAssignment);
        when(assignmentConverter.toDto(mockAssignment, 0)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);

            // Act
            AssignmentResponseDto result = assignmentService.createAssignment(validRequest, teacherId);

            // Assert
            assertNotNull(result);
            verify(corsiClient, never()).isTeacherOfCourse(anyString(), anyString());
            verify(assignmentRepository, times(1)).save(any(Assignment.class));
        }
    }

    // ==================== FIND BY ID ====================

    @Test
    @DisplayName("findById - Successo: trova assignment per ID (docente proprietario)")
    void testFindById_Success_Teacher() {
        // Arrange
        String assignmentId = "assignment-789";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.countByAssignmentId(assignmentId)).thenReturn(5);
        when(assignmentConverter.toDto(mockAssignment, 5)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            AssignmentResponseDto result = assignmentService.findById(assignmentId, teacherId);

            // Assert
            assertNotNull(result);
            assertEquals("assignment-789", result.id());
            verify(assignmentRepository, times(1)).findById(assignmentId);
        }
    }

    @Test
    @DisplayName("findById - Errore: assignment non trovato")
    void testFindById_NotFound() {
        // Arrange
        String assignmentId = "non-existent";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.findById(assignmentId, teacherId));

        assertTrue(exception.getMessage().contains("Compito non trovato"));
    }

    @Test
    @DisplayName("findById - Errore: accesso negato (utente non autorizzato)")
    void testFindById_AccessDenied() {
        // Arrange
        String assignmentId = "assignment-789";
        String unauthorizedUserId = "other-user";

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(iscrizioniClient.isStudentEnrolled(unauthorizedUserId, courseId)).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> assignmentService.findById(assignmentId, unauthorizedUserId));

            assertTrue(exception.getMessage().contains("Non sei autorizzato"));
        }
    }

    @Test
    @DisplayName("findById - Successo: admin bypassa controlli")
    void testFindById_AdminBypass() {
        // Arrange
        String assignmentId = "assignment-789";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.countByAssignmentId(assignmentId)).thenReturn(3);
        when(assignmentConverter.toDto(mockAssignment, 3)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);

            // Act
            AssignmentResponseDto result = assignmentService.findById(assignmentId, "any-user");

            // Assert
            assertNotNull(result);
            verify(iscrizioniClient, never()).isStudentEnrolled(anyString(), anyString());
        }
    }

    // ==================== GET ASSIGNMENTS BY COURSE ====================

    @Test
    @DisplayName("getAssignmentsByCourse - Successo: docente del corso")
    void testGetAssignmentsByCourse_Teacher() {
        // Arrange
        Assignment assignment2 = new Assignment();
        assignment2.setId("assignment-999");
        assignment2.setCourseId(courseId);

        List<Assignment> assignments = Arrays.asList(mockAssignment, assignment2);

        when(corsiClient.isTeacherOfCourse(courseId, teacherId)).thenReturn(true);
        when(assignmentRepository.findByCourseId(courseId)).thenReturn(assignments);
        when(submissionRepository.countByAssignmentId(anyString())).thenReturn(2);
        when(assignmentConverter.toDto(any(Assignment.class), anyInt())).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            List<AssignmentResponseDto> result = assignmentService.getAssignmentsByCourse(courseId, teacherId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(assignmentRepository, times(1)).findByCourseId(courseId);
        }
    }

    @Test
    @DisplayName("getAssignmentsByCourse - Successo: studente iscritto")
    void testGetAssignmentsByCourse_Student() {
        // Arrange
        String studentId = "student-001";
        List<Assignment> assignments = Arrays.asList(mockAssignment);

        when(corsiClient.isTeacherOfCourse(courseId, studentId)).thenReturn(false);
        when(iscrizioniClient.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(assignmentRepository.findByCourseId(courseId)).thenReturn(assignments);
        when(submissionRepository.countByAssignmentId(anyString())).thenReturn(1);
        when(assignmentConverter.toDto(any(Assignment.class), anyInt())).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            List<AssignmentResponseDto> result = assignmentService.getAssignmentsByCourse(courseId, studentId);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("getAssignmentsByCourse - Errore: utente non autorizzato")
    void testGetAssignmentsByCourse_Unauthorized() {
        // Arrange
        String unauthorizedUser = "unauthorized-user";

        when(corsiClient.isTeacherOfCourse(courseId, unauthorizedUser)).thenReturn(false);
        when(iscrizioniClient.isStudentEnrolled(unauthorizedUser, courseId)).thenReturn(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> assignmentService.getAssignmentsByCourse(courseId, unauthorizedUser));

            assertTrue(exception.getMessage().contains("Non sei autorizzato"));
            verify(assignmentRepository, never()).findByCourseId(anyString());
        }
    }

    // ==================== UPDATE ASSIGNMENT ====================

    @Test
    @DisplayName("updateAssignment - Successo: modifica assignment")
    void testUpdateAssignment_Success() {
        // Arrange
        String assignmentId = "assignment-789";
        AssignmentUpdateDto updateDto = new AssignmentUpdateDto(
                "Updated Title",
                "Updated Description",
                LocalDateTime.now().plusDays(10));

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(mockAssignment);
        when(submissionRepository.countByAssignmentId(assignmentId)).thenReturn(3);
        when(assignmentConverter.toDto(mockAssignment, 3)).thenReturn(mockResponse);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            AssignmentResponseDto result = assignmentService.updateAssignment(assignmentId, updateDto, teacherId);

            // Assert
            assertNotNull(result);
            verify(assignmentConverter, times(1)).updateEntity(mockAssignment, updateDto);
            verify(assignmentRepository, times(1)).save(mockAssignment);
            verify(eventPublisher, times(1)).publishAssignmentUpdated(mockAssignment);
        }
    }

    @Test
    @DisplayName("updateAssignment - Errore: assignment non trovato")
    void testUpdateAssignment_NotFound() {
        // Arrange
        String assignmentId = "non-existent";
        AssignmentUpdateDto updateDto = new AssignmentUpdateDto(
                "Title",
                "Description",
                LocalDateTime.now().plusDays(5));

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.updateAssignment(assignmentId, updateDto, teacherId));

        assertTrue(exception.getMessage().contains("Compito non trovato"));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAssignment - Errore: docente non proprietario")
    void testUpdateAssignment_NotOwner() {
        // Arrange
        String assignmentId = "assignment-789";
        String otherTeacherId = "other-teacher";
        AssignmentUpdateDto updateDto = new AssignmentUpdateDto(
                "Title",
                "Description",
                LocalDateTime.now().plusDays(5));

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> assignmentService.updateAssignment(assignmentId, updateDto, otherTeacherId));

            assertTrue(exception.getMessage().contains("Non hai i permessi"));
            verify(assignmentRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("updateAssignment - Errore: data di scadenza passata")
    void testUpdateAssignment_PastDueDate() {
        // Arrange
        String assignmentId = "assignment-789";
        AssignmentUpdateDto updateDto = new AssignmentUpdateDto(
                "Title",
                "Description",
                LocalDateTime.now().minusDays(1));

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> assignmentService.updateAssignment(assignmentId, updateDto, teacherId));

            assertEquals("La data di scadenza deve essere futura", exception.getMessage());
            verify(assignmentRepository, never()).save(any());
        }
    }

    // ==================== DELETE ASSIGNMENT ====================

    @Test
    @DisplayName("deleteAssignmentById - Successo: elimina assignment e cascata")
    void testDeleteAssignmentById_Success() {
        // Arrange
        String assignmentId = "assignment-789";

        Submission submission1 = new Submission();
        submission1.setId("submission-001");
        Submission submission2 = new Submission();
        submission2.setId("submission-002");
        List<Submission> submissions = Arrays.asList(submission1, submission2);

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        when(submissionRepository.findByAssignmentId(assignmentId)).thenReturn(submissions);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act
            assignmentService.deleteAssignmentById(assignmentId, teacherId);

            // Assert
            verify(eventPublisher, times(1)).publishAssignmentDeleted(
                    eq(assignmentId),
                    eq(courseId),
                    eq(teacherId),
                    eq("Test Assignment"));
            verify(fileStorageService, times(2)).deleteAllFilesByEntity(
                    eq(FileEntityType.SUBMISSION),
                    anyString());
            verify(fileStorageService, times(1)).deleteAllFilesByEntity(
                    eq(FileEntityType.ASSIGNMENT),
                    eq(assignmentId));
            verify(submissionRepository, times(1)).deleteByAssignmentId(assignmentId);
            verify(assignmentRepository, times(1)).deleteById(assignmentId);
        }
    }

    @Test
    @DisplayName("deleteAssignmentById - Errore: assignment non trovato")
    void testDeleteAssignmentById_NotFound() {
        // Arrange
        String assignmentId = "non-existent";
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.deleteAssignmentById(assignmentId, teacherId));

        assertTrue(exception.getMessage().contains("Compito non trovato"));
        verify(assignmentRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteAssignmentById - Errore: docente non proprietario")
    void testDeleteAssignmentById_NotOwner() {
        // Arrange
        String assignmentId = "assignment-789";
        String otherTeacherId = "other-teacher";

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);

            // Act & Assert
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> assignmentService.deleteAssignmentById(assignmentId, otherTeacherId));

            assertTrue(exception.getMessage().contains("Non hai i permessi"));
            verify(assignmentRepository, never()).deleteById(anyString());
        }
    }
}
