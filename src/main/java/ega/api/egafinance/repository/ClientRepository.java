package ega.api.egafinance.repository;

import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByIdentifiantAndEmail(String identifiant, String email);


    // Recherche partielle par email (RECOMMANDÉ pour l'UX)
    List<Client> findByEmailContainingIgnoreCase(String email);

    // Recherche par email insensible à la casse
    Optional<Client> findByEmailIgnoreCase(String email);

    // BONUS: Recherche par nom ou prénom (si besoin plus tard)
    List<Client> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);
}
