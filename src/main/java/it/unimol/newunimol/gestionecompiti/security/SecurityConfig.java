package it.unimol.newunimol.gestionecompiti.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurazione centrale di Spring Security per il microservizio.
 * 
 * Responsabilità:
 * 1. Registra il JwtAuthenticationFilter nella catena di filtri
 * 2. Definisce quali endpoint richiedono autenticazione
 * 3. Configura la gestione delle sessioni (STATELESS per JWT)
 * 4. Disabilita CSRF (non necessario per API REST stateless)
 * 5. Abilita @PreAuthorize per controlli granulari nei controller (Passo 6)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Abilita @PreAuthorize, @Secured, etc. (useremo domani nel Passo 6)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configura la catena di filtri di sicurezza.
     * 
     * Spring Security 6+ usa un approccio funzionale con lambda DSL.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disabilita CSRF: non serve per API REST stateless con JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configura le regole di autorizzazione per gli endpoint
            .authorizeHttpRequests(authorize -> authorize
                // Endpoint pubblici (accessibili senza autenticazione)
                .requestMatchers(
                    "/h2-console/**",        // Console H2 per sviluppo
                    "/swagger-ui/**",        // Swagger UI
                    "/v3/api-docs/**",       // OpenAPI docs
                    "/swagger-ui.html"       // Swagger home
                ).permitAll()
                
                // Tutti gli altri endpoint richiedono autenticazione
                .anyRequest().authenticated()
            )
            
            // Configura la gestione delle sessioni
            .sessionManagement(session -> session
                // STATELESS: Spring Security non crea/usa sessioni HTTP
                // Ogni richiesta deve contenere il token JWT
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Aggiungi il filtro JWT prima del filtro di autenticazione standard
            // Questo permette di intercettare le richieste prima e settare il SecurityContext
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Configurazione speciale per H2 Console (permette iframe)
        http.headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.sameOrigin())
        );
        
        return http.build();
    }
}
