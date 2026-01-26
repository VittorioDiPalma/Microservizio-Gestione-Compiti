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
 * Usa la chiave pubblica RSA per validare i token generati dal microservizio
 * Gestione-Utenti-e-Ruoli.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.public-key}")
    private String publicKeyString;

    private PublicKey publicKey;

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

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
