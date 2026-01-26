package it.unimol.newunimol.gestionecompiti.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Utility per estrarre informazioni sull'utente autenticato dal
 * SecurityContext.
 */
@Component
public class SecurityUtils {

    /**
     * Estrae l'ID dell'utente autenticato dal token JWT.
     * 
     * @return ID utente (sub claim del JWT)
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Utente non autenticato");
        }
        return authentication.getName();
    }

    /**
     * Estrae il ruolo dell'utente autenticato.
     * 
     * @return Ruolo (es. "ROLE_DOCENTE", "ROLE_STUDENTE")
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities.isEmpty()) {
            return null;
        }

        // Restituisce il primo ruolo (dovrebbe essercene solo uno)
        return authorities.iterator().next().getAuthority();
    }

    /**
     * Verifica se l'utente autenticato ha un determinato ruolo.
     * 
     * @param role Ruolo da verificare (es. "ROLE_DOCENTE")
     * @return true se l'utente ha quel ruolo
     */
    public static boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }

    /**
     * Verifica se l'utente è un TEACHER (docente).
     */
    public static boolean isTeacher() {
        return hasRole("ROLE_TEACHER");
    }

    /**
     * Verifica se l'utente è uno STUDENT (studente).
     */
    public static boolean isStudent() {
        return hasRole("ROLE_STUDENT");
    }

    /**
     * Verifica se l'utente è un ADMIN.
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
