package ega.api.egafinance.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    // ✅ Clé sécurisée pour signer les tokens
    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ✅ Extraire le username depuis le token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extraire une revendication spécifique depuis le token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ✅ Analyse du contenu du token (body + signature)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ Générez un token avec un rôle unique
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Récupérez le rôle unique (au lieu d'une liste de rôles)
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Exemple : ROLE_USER
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r) // Supprime ROLE_
                .findFirst() // Si un seul rôle, prenez le premier
                .orElse("USER"); // Valeur par défaut si aucun rôle fourni

        claims.put("role", role); // Ajout du rôle unique

        return Jwts.builder()
                .setClaims(claims) // Ajout des revendications
                .setSubject(userDetails.getUsername()) // Définit l'utilisateur (email/username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Date d'émission
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Date d'expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signature
                .compact(); // Génère le token JWT
    }


    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // ✅ Vérifiez si le token est expiré
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ✅ Extraire la date d'expiration
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ✅ Extraire les rôles depuis le token
    // ✅ Extraire le rôle unique depuis le token
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class); // Retourne le rôle unique (exemple : "USER" ou "ADMIN")
    }
}