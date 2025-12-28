package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.User;
import ega.api.egafinance.repository.UserRepository;
import ega.api.egafinance.service.AuthService;
import ega.api.egafinance.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @MutationMapping
    @PreAuthorize("permitAll()")
    public User createUser(@Valid @Argument("input") UserRegisterInput userRegisterInput) {
        return authService.Register(userRegisterInput);
    }

    @MutationMapping
    public String login(@Argument String email, @Argument String password) {
        try {
            // Authentifie l'utilisateur avec son email et mot de passe
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }

        // Récupérez l'utilisateur depuis la base de données
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ajouter le rôle unique comme autorité pour Spring Security
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // Générer le token JWT avec un seul rôle
        return jwtUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        List.of(authority) // Un seul rôle
                )
        );
    }


}