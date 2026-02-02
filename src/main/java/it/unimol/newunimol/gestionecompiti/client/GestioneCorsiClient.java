package it.unimol.newunimol.gestionecompiti.client;

import it.unimol.newunimol.gestionecompiti.client.dto.CorsoResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client HTTP per comunicare con il microservizio Gestione-Corsi.
 */
@Service
public class GestioneCorsiClient {

    @Value("${gestione-corsi.url}")
    private String baseUrl;

    private final WebClient webClient;

    public GestioneCorsiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8082")
                .build();
    }

    /**
     * Ottiene i dettagli di un corso per ID.
     * 
     * @param courseId ID del corso da recuperare
     * @return CorsoResponseDTO con i dati del corso
     * @throws CourseNotFoundException se il corso non esiste (404)
     * @throws CourseServiceException  se c'è un errore di comunicazione
     */
    public CorsoResponseDTO getCorsoById(String courseId) {
        try {
            CorsoResponseDTO corso = webClient
                    .get()
                    .uri(baseUrl + "/api/v1/corsi/{id}", courseId)
                    .retrieve()
                    .bodyToMono(CorsoResponseDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return corso;

        } catch (WebClientResponseException.NotFound e) {
            throw new CourseNotFoundException("Corso non trovato con ID: " + courseId);

        } catch (WebClientResponseException e) {
            throw new CourseServiceException(
                    "Errore nella comunicazione con Gestione-Corsi: " + e.getMessage());

        } catch (Exception e) {
            throw new CourseServiceException(
                    "Errore imprevisto nella comunicazione con Gestione-Corsi: " + e.getMessage());
        }
    }

    /**
     * Verifica se un corso esiste.
     * 
     * @param courseId ID del corso da verificare
     * @return true se il corso esiste, false altrimenti
     */
    public boolean courseExists(String courseId) {
        try {
            getCorsoById(courseId);
            return true;
        } catch (CourseNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifica che un docente insegni un determinato corso.
     * 
     * @param courseId  ID del corso
     * @param teacherId ID del docente (matricola)
     * @return true se il docente insegna il corso, false altrimenti
     */
    public boolean isTeacherOfCourse(String courseId, String teacherId) {
        try {
            CorsoResponseDTO corso = getCorsoById(courseId);

            if (corso.getDocenti() == null || corso.getDocenti().isEmpty()) {
                return false;
            }

            boolean isTeacher = corso.getDocenti().stream()
                    .anyMatch(docente -> docente.getId().equals(teacherId));

            return isTeacher;
        } catch (CourseNotFoundException e) {
            return false;
        }
    }

    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(String message) {
            super(message);
        }
    }

    public static class CourseServiceException extends RuntimeException {
        public CourseServiceException(String message) {
            super(message);
        }
    }
}
