package it.unimol.newunimol.gestionecompiti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro che intercetta ogni richiesta HTTP in arrivo per validare il token JWT.
 * 
 * Funzionamento:
 * 1. Estrae il token dall'header Authorization (formato: "Bearer <token>")
 * 2. Valida il token usando JwtTokenProvider
 * 3. Se valido, estrae userId, username e role
 * 4. Crea un'autenticazione Spring Security e la inserisce nel SecurityContext
 * 5. Passa la richiesta al filtro successivo nella catena
 * 
 * Questo filtro si applica a TUTTE le richieste HTTP, indipendentemente dalla provenienza.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Estrae il token JWT dall'header Authorization
            String jwt = extractJwtFromRequest(request);
            
            // 2. Se il token esiste e è valido
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                
                // 3. Estrae le informazioni dal token
                String userId = jwtTokenProvider.extractUserId(jwt);
                String username = jwtTokenProvider.extractUsername(jwt);
                String role = jwtTokenProvider.extractRole(jwt);
                
                // 4. Crea l'authority (ruolo) per Spring Security
                // Spring Security richiede che i ruoli inizino con "ROLE_"
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                
                // 5. Crea un'autenticazione Spring Security
                // Usiamo userId come principal (identità utente), nessuna credential (null),
                // e la lista delle authorities (ruoli)
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,  // principal (chi è l'utente)
                        null,    // credentials (non servono, già validato il token)
                        List.of(authority)  // authorities (ruoli dell'utente)
                    );
                
                // 6. Aggiunge dettagli della richiesta HTTP all'autenticazione
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 7. Inserisce l'autenticazione nel SecurityContext
                // Questo comunica a Spring Security che l'utente è autenticato
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Log per debugging (opzionale)
                logger.debug("Utente autenticato: userId=" + userId + ", username=" + username + ", role=" + role);
            }
            
        } catch (Exception ex) {
            // Se c'è qualsiasi errore nel parsing/validazione del token,
            // logghiamo l'errore ma NON blocchiamo la richiesta
            logger.error("Errore durante l'autenticazione JWT: " + ex.getMessage(), ex);
            // Il SecurityContext rimane vuoto, quindi l'utente risulterà non autenticato
        }
        
        // 8. Passa la richiesta al filtro successivo nella catena
        // (che potrebbe essere un altro filtro di sicurezza o il controller finale)
        filterChain.doFilter(request, response);
    }
    
    /**
     * Estrae il token JWT dall'header Authorization.
     * 
     * Formato atteso: "Authorization: Bearer <token>"
     * 
     * @param request la richiesta HTTP
     * @return il token JWT estratto, oppure null se non presente o in formato errato
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Verifica che l'header esista e inizi con "Bearer "
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // Rimuove "Bearer " (7 caratteri) e restituisce solo il token
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
