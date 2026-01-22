package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ActivationResponse;
import ega.api.egafinance.dto.AuthResponse;
import ega.api.egafinance.dto.MeResponse;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.User;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.repository.UserRepository;
import ega.api.egafinance.service.AuthService;
import ega.api.egafinance.service.ClientService;
import ega.api.egafinance.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ClientService clientService;

    @MutationMapping
    @PreAuthorize("permitAll()")
    public User createUser(@Valid @Argument("input") UserRegisterInput userRegisterInput) {
        return authService.Register(userRegisterInput);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public MeResponse me() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName();
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (role.equals("ROLE_CLIENT")) {
            Client client = clientService.getOneClientByEmail(email)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Client connecté introuvable"));
            return MeResponse.fromClient(client);
        }

        throw new AccessDeniedException("Rôle non supporté pour la requête me");
    }


    @MutationMapping
    public AuthResponse login(@Argument String email, @Argument String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        String token = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(authority)
        ));
        return new AuthResponse(token, user);
    }

    @MutationMapping
    public ActivationResponse completeActivation(@Argument String token, @Argument String password) {
        try {
            authService.completeActivation(token, password);
            return new ActivationResponse(true, "Un email d'activation a été envoyé avec succès.");
        } catch (RuntimeException e) {
            return new ActivationResponse(false, e.getMessage());
        } catch (Exception e) {
            return new ActivationResponse(false, "Une erreur technique est survenue. Veuillez réessayer plus tard.");
        }
    }

    @MutationMapping
    public ActivationResponse initiateActivation(@Argument String identifiant, @Argument String email) {
        try {
            authService.initiateActivation(identifiant, email);
            return new ActivationResponse(true, "Un email d'activation a été envoyé avec succès.");
        } catch (RuntimeException e) {
            return new ActivationResponse(false, e.getMessage());
        } catch (Exception e) {
            return new ActivationResponse(false, "Une erreur technique est survenue. Veuillez réessayer plus tard.");
        }
    }
}