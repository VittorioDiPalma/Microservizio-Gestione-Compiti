package it.unimol.newunimol.gestionecompiti.exception;

import it.unimol.newunimol.gestionecompiti.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Gestisce EntityNotFoundException (404 Not Found)
         */
        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFound(
                        EntityNotFoundException ex,
                        HttpServletRequest request) {
                LOGGER.warn("Entity not found: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Gestisce AccessDeniedException (403 Forbidden)
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {
                LOGGER.warn("Access denied: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Gestisce IllegalArgumentException (400 Bad Request)
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                LOGGER.warn("Bad request: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Gestisce errori di validazione DTO (400 Bad Request)
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                LOGGER.warn("Validation error: {}", errorMessage);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Failed",
                                errorMessage,
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Gestisce file troppo grande (413 Payload Too Large)
         */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxSizeException(
                        MaxUploadSizeExceededException ex,
                        HttpServletRequest request) {
                LOGGER.warn("File too large: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                                "Payload Too Large",
                                "Il file supera la dimensione massima consentita (10MB)",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
        }

        /**
         * Gestisce body della richiesta malformato o con valori null (400 Bad Request)
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                LOGGER.warn("Invalid request body: {}", ex.getMessage());

                String message = "Il corpo della richiesta non è valido. "
                                + "Verifica che tutti i campi obbligatori siano presenti e che i tipi di dato siano corretti.";

                // Estrai messaggio più specifico se disponibile
                if (ex.getMessage() != null) {
                        if (ex.getMessage().contains("Cannot deserialize")) {
                                message = "Tipo di dato non valido nel corpo della richiesta.";
                        } else if (ex.getMessage().contains("Required request body is missing")) {
                                message = "Il corpo della richiesta è obbligatorio.";
                        }
                }

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                message,
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Gestisce NullPointerException causati da valori null inattesi (400 Bad Request)
         */
        @ExceptionHandler(NullPointerException.class)
        public ResponseEntity<ErrorResponse> handleNullPointer(
                        NullPointerException ex,
                        HttpServletRequest request) {
                LOGGER.error("NullPointerException: ", ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                "Uno o più campi obbligatori sono mancanti o null.",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Gestisce DataIntegrityViolationException per violazioni dei constraint del database (400 Bad Request)
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                LOGGER.error("Data integrity violation: ", ex);

                String message = "Violazione dei vincoli di integrità dei dati";

                // Estrai messaggio più specifico se disponibile
                if (ex.getMessage() != null) {
                        if (ex.getMessage().contains("not-null")) {
                                message = "Uno o più campi obbligatori sono mancanti o null";
                        } else if (ex.getMessage().contains("unique")) {
                                message = "Violazione del vincolo di unicità";
                        } else if (ex.getMessage().contains("foreign key")) {
                                message = "Riferimento a un'entità inesistente";
                        }
                }

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                message,
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Gestisce tutte le altre eccezioni (500 Internal Server Error)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                LOGGER.error("Unexpected error: ", ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "Si è verificato un errore interno.",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
