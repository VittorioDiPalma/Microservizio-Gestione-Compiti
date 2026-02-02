package it.unimol.newunimol.gestionecompiti.dto;

public record SubmissionStatsDto(
    String assignmentId,
    int totalSubmissions,
    int onTimeSubmissions,
    int lateSubmissions,
    int gradedSubmissions,
    int pendingSubmissions,
    int totalEnrolledStudents,
    int studentsMissing,
    double completionRate
) {
    
    public SubmissionStatsDto {
        if (totalSubmissions != onTimeSubmissions + lateSubmissions + gradedSubmissions + pendingSubmissions) {
            throw new IllegalArgumentException(
                "Inconsistenza nelle statistiche: total=" + totalSubmissions +
                " ma onTime+late+graded+pending=" + (onTimeSubmissions + lateSubmissions + gradedSubmissions + pendingSubmissions)
            );
        }
        
        if (totalEnrolledStudents < 0 || studentsMissing < 0) {
            throw new IllegalArgumentException("I conteggi degli studenti non possono essere negativi");
        }
        
        if (studentsMissing > totalEnrolledStudents) {
            throw new IllegalArgumentException(
                "Studenti mancanti (" + studentsMissing + ") non può essere maggiore degli iscritti (" + totalEnrolledStudents + ")"
            );
        }
        
        if (completionRate < 0.0 || completionRate > 100.0) {
            throw new IllegalArgumentException("Percentuale completamento deve essere tra 0 e 100");
        }
    }
}
