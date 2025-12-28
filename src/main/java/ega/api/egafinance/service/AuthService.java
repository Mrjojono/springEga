package ega.api.egafinance.service;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.User;
import ega.api.egafinance.mapper.ClientMapper;
import ega.api.egafinance.mapper.UserMapper;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ClientMapper clientMapper;


    /**
     * Inscrit un utilisateur dans le système
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
        User user = userMapper.toUser(userRegisterInput);

        user.setPassword(passwordEncoder.encode(userRegisterInput.getPassword()));
        user.setRole(User.Role.USER);

        return userRepository.save(user);
    }


    /**
     * Met à jour un utilisateur existant
     *
     * @param userId            Identifiant de l'utilisateur à mettre à jour
     * @param userRegisterInput Nouvelles données de l'utilisateur
     * @return l'utilisateur mis à jour
     */
    public User updateUser(String userId, UserRegisterInput userRegisterInput) {
        // Récupère l'utilisateur existant depuis la base de données
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Utilise le mapper pour mettre à jour les propriétés de l'utilisateur
        userMapper.register(userRegisterInput, existingUser);

        // Sauvegarde et retourne l'utilisateur mis à jour
        return userRepository.save(existingUser);
    }
}