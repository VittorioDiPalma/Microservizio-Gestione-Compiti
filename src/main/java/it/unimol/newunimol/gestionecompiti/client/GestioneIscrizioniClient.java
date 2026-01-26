package it.unimol.newunimol.gestionecompiti.client;

import it.unimol.newunimol.gestionecompiti.client.dto.CourseEnrollmentDto;
import it.unimol.newunimol.gestionecompiti.client.dto.EnrollmentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Client HTTP per comunicare con il microservizio Gestione-Iscrizioni.
 */
@Service
public class GestioneIscrizioniClient {

    @Value("${gestione-iscrizioni.url}")
    private String baseUrl;

    private final WebClient webClient;

    public GestioneIscrizioniClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8083")
                .build();
    }

    public List<CourseEnrollmentDto> getCourseEnrollments(String courseId) {

        try {
            List<CourseEnrollmentDto> enrollments = webClient
                    .get()
                    .uri(baseUrl + "/api/v1/admin/courses/{courseId}/enrollments", courseId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CourseEnrollmentDto>>() {
                    })
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return enrollments != null ? enrollments : List.of();

        } catch (WebClientResponseException e) {
            throw new EnrollmentServiceException(
                    "Errore nella comunicazione con Gestione-Iscrizioni: " + e.getMessage());

        } catch (Exception e) {
            throw new EnrollmentServiceException(
                    "Errore imprevisto nella comunicazione con Gestione-Iscrizioni: " + e.getMessage());
        }
    }

    public boolean isStudentEnrolled(String studentId, String courseId) {

        try {
            List<CourseEnrollmentDto> enrollments = getCourseEnrollments(courseId);

            boolean isEnrolled = enrollments.stream()
                    .anyMatch(e -> e.getStudentId().equals(studentId)
                            && e.getStatus() == EnrollmentStatus.ACTIVE);
            return isEnrolled;

        } catch (EnrollmentServiceException e) {
            return false;
        }
    }

    public List<String> getEnrolledStudentIds(String courseId) {
        List<CourseEnrollmentDto> enrollments = getCourseEnrollments(courseId);

        List<String> studentIds = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(CourseEnrollmentDto::getStudentId)
                .toList();
        return studentIds;
    }

    public static class EnrollmentServiceException extends RuntimeException {
        public EnrollmentServiceException(String message) {
            super(message);
        }
    }
}
