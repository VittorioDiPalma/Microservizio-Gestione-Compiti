package it.unimol.newunimol.gestionecompiti.exception;

import it.unimol.newunimol.gestionecompiti.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Gestisce EntityNotFoundException (404 Not Found)
         */
        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFound(
                        EntityNotFoundException ex,
                        HttpServletRequest request) {
                logger.warn("Entity not found: {}", ex.getMessage());

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
                logger.warn("Access denied: {}", ex.getMessage());

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
                logger.warn("Bad request: {}", ex.getMessage());

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

                logger.warn("Validation error: {}", errorMessage);

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
                logger.warn("File too large: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                                "Payload Too Large",
                                "Il file supera la dimensione massima consentita (10MB)",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
        }

        /**
         * Gestisce tutte le altre eccezioni (500 Internal Server Error)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                logger.error("Unexpected error: ", ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "Si è verificato un errore interno.",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
