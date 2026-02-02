package it.unimol.newunimol.gestionecompiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record AssignmentRequestDto(
                @NotBlank(message = "Il titolo è obbligatorio") String title,

                @NotBlank(message = "La descrizione è obbligatoria") String description,

                @NotNull(message = "La data di scadenza è obbligatoria") @Future(message = "La data di scadenza deve essere futura") LocalDateTime dueDate,

                @NotBlank(message = "L'ID del corso è obbligatorio") String courseId) {
}
