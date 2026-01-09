package it.unimol.newunimol.gestionecompiti.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Provider per la gestione e validazione dei token JWT.
 * Usa la chiave pubblica RSA per validare i token generati dal microservizio Utenti.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.public-key}")
    private String publicKeyString;

    private PublicKey publicKey;

    /**
     * Carica e restituisce la chiave pubblica RSA per validare i token JWT.
     */
    private PublicKey getPublicKey() {
        if (this.publicKey == null) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(this.publicKeyString);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.publicKey = keyFactory.generatePublic(spec);
            } catch (Exception e) {
                throw new RuntimeException("Errore nel caricamento della chiave pubblica JWT", e);
            }
        }
        return publicKey;
    }

    /**
     * Estrae tutti i claims dal token JWT.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Estrae l'ID utente dal token (campo "sub").
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Estrae il nome utente dal token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    /**
     * Estrae il ruolo dell'utente dal token.
     * Possibili valori: DOCENTE, STUDENTE, ADMIN, SUPER_ADMIN
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Estrae la data di scadenza dal token.
     */
    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Verifica se il token è scaduto.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valida il token JWT verificando:
     * 1. La firma è corretta (con chiave pubblica)
     * 2. Il token non è scaduto
     * 
     * @param token Il token JWT da validare
     * @return true se il token è valido, false altrimenti
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
