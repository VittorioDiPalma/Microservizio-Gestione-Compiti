package it.unimol.newunimol.gestionecompiti.dto;

public record SubmissionStatsDto(
    String assignmentId,
    int totalSubmissions,     // Consegne totali
    int onTimeSubmissions,    // Consegne in tempo
    int lateSubmissions       // Consegne in ritardo
) {}