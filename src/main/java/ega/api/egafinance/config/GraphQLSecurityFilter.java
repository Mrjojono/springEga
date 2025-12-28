package ega.api.egafinance.config;

import ega.api.egafinance.service.CustomUserDetailsService;
import ega.api.egafinance.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j // Pour les logs
public class GraphQLSecurityFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        log.info("=== GraphQLSecurityFilter ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Authorization Header: {}", authorizationHeader);

        String jwt = null; // JWT extrait
        String username = null; // Nom d'utilisateur extrait du token

        // ✅ Étape 1 : Extraction du token JWT depuis le header Authorization
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Retirer le préfixe "Bearer "
            log.info("Token JWT extrait : {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");

            try {
                // Extraction du username depuis le JWT
                username = jwtUtil.extractUsername(jwt);
                log.info("Nom d'utilisateur extrait du token : {}", username);

            } catch (JwtException e) {
                log.error("Erreur lors de l'extraction des informations JWT : {}", e.getMessage());
            }
        } else {
            log.warn("Aucun token JWT trouvé dans le header Authorization !");
        }

        // ✅ Étape 2 : Valider et authentifier le token s'il est présent
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Charger les détails utilisateur depuis la base (via email extrait du token)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("Détails utilisateur chargé : {}", userDetails.getUsername());
                log.info("Autorité unique pour l'utilisateur : {}", userDetails.getAuthorities());

                // Valider que le token correspond aux détails utilisateur
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    log.info("Token JWT valide ✅");

                    // Création d'une autorité basée sur le rôle (ajouter le préfixe ROLE_)
                    String roleFromToken = jwtUtil.extractRole(jwt);
                    log.info("Rôle extrait du token : {}", roleFromToken);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleFromToken);

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    List.of(authority) // Définir l'autorité basée sur le rôle
                            );

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    log.info("Contexte de sécurité mis à jour : {}", SecurityContextHolder.getContext().getAuthentication());
                } else {
                    log.warn("Token JWT invalide ❌");
                }

            } catch (Exception e) {
                // Capturer et logger toute exception inattendue
                log.error("Erreur lors de l'authentification : {}", e.getMessage(), e);
            }
        }


        // ✅ Étape 3 : Filtrer la requête (poursuivre la chaîne de filtres)
        filterChain.doFilter(request, response);
    }


}