package it.unimol.newunimol.gestionecompiti.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionRequestDto(
        @NotBlank(message = "Il contenuto è obbligatorio") String content) {
}
