package ega.api.egafinance.service;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.ActivationToken;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.User;
import ega.api.egafinance.mapper.ClientMapper;
import ega.api.egafinance.mapper.UserMapper;
import ega.api.egafinance.repository.ActivationTokenRepository;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ActivationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ClientMapper clientMapper;
    private final EmailService emailService;

    /**
     * Inscrit un utilisateur dans le système en tant que client en meme temps
     * la particularité c'est que une personne peut creer un compte et devenir un nouveau client directement
     *
     * @param userRegisterInput Données d'inscription de l'utilisateur
     * @return l'utilisateur enregistré
     */
    @Override
    public User Register(UserRegisterInput userRegisterInput) {

        // Vérifie si l'email est déjà utilisé
        if (userRepository.findByEmail(userRegisterInput.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already in use");
        }
        Client client = clientMapper.toClientRegister(userRegisterInput);

        client.setPassword(passwordEncoder.encode(userRegisterInput.getPassword()));
        client.setRole(User.Role.CLIENT);

        return clientRepository.save(client);
    }


    /**
     * Met à jour un utilisateur existant
     *
     * @param userId            Identifiant de l'utilisateur à mettre à jour
     * @param userRegisterInput Nouvelles données de l'utilisateur
     *                          ici on ne peut mettre a jour que le mot de passe et l'email
     * @return l'utilisateur mis à jour
     */
    public User updateUser(String userId, UserRegisterInput userRegisterInput) {
        // Récupère l'utilisateur existant depuis la base de données
        User existingUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Utilise le mapper pour mettre à jour les propriétés de l'utilisateur
        userMapper.register(userRegisterInput, existingUser);

        // Sauvegarde et retourne l'utilisateur mis à jour
        return userRepository.save(existingUser);
    }


    @Transactional
    public void initiateActivation(String identifiant, String email) {

        Client client = clientRepository.findByIdentifiantAndEmail(identifiant, email)
                .orElseThrow(() -> new RuntimeException("Identifiant ou Email incorrect"));

        // 3. Générer un token d'activation unique
        String token = UUID.randomUUID().toString();


        tokenRepository.deleteByUserId(client.getId());
        ActivationToken activationToken = new ActivationToken(client, token);
        tokenRepository.save(activationToken);

        // 4. Envoyer l'email
        String activationUrl = "http://localhost:4200/activate-account?token=" + token;
        emailService.sendActivationEmail(client, activationUrl);
    }

    @Transactional
    public User completeActivation(String token, String newPassword) {

        ActivationToken activationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lien invalide ou déjà utilisé"));

        if (activationToken.isExpired()) {
            tokenRepository.delete(activationToken);
            throw new RuntimeException("Le lien a expiré (24h)");
        }

        User user = activationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
        tokenRepository.delete(activationToken);

        return user;
    }
}