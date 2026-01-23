package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.dto.ComptePublicInfo;
import ega.api.egafinance.dto.CompteUpdateInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.StatutCompte;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.service.ClientService;
import ega.api.egafinance.service.CompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CompteController {


    private final CompteService compteService;
    private final ClientService clientService;
    private final ClientRepository clientRepository;


    @QueryMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Compte> comptes(@Argument Integer page, @Argument Integer size) {
        int pageIndex = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return compteService.getPagedCompte(pageable);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Compte createCompte(@Argument("input") @Valid CompteInput compteInput) {
        Optional<Client> client = clientService.getClient(compteInput.getProprietaireId());
        if (client.isEmpty()) {
            throw new ResourceNotFoundException("Le propriétaire du compte n'existe pas !");
        }
        return compteService.saveCompte(compteInput);
    }


    @SchemaMapping(typeName = "Compte", field = "proprietaire")
    public Client proprietaire(Compte compte) {
        return compte.getClient();
    }

    @MutationMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Boolean deleteCompte(@Argument String id) {
        return compteService.deleteCompte(id);
    }


    @MutationMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Compte updateCompte(@Argument String id, @Argument("compte") @Valid CompteUpdateInput compteUpdateInput) {
        return compteService.updateCompte(id, compteUpdateInput);
    }


    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Compte> comptesParClientId(@Argument String clientId) {
        try {
            final String id = clientId.trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("L'identifiant du client ne peut pas être vide.");
            }
            Client client = clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id : " + id));


            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new AccessDeniedException("Utilisateur non authentifié.");
            }


            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_AGENT_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

            if (!isAdmin) {
                String principalName = auth.getName();
                String clientEmail = (client.getEmail() == null) ? null : client.getEmail().trim();

                if (principalName == null || !principalName.equalsIgnoreCase(clientEmail)) {
                    throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter les comptes de ce client.");
                }
            }

            return compteService.findComptesByClientId(id);

        } catch (ResourceNotFoundException | AccessDeniedException | IllegalArgumentException ex) {

            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des comptes du client.", ex);
        }
    }



    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ComptePublicInfo> searchComptesForTransfer(@Argument String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return Collections.emptyList(); // Retourner liste vide au lieu d'erreur
            }

            String searchEmail = email.trim();
            List<ComptePublicInfo> result = new ArrayList<>();

            // Recherche partielle - trouve tous les emails qui contiennent le texte
            List<Client> clients = clientRepository.findByEmailContainingIgnoreCase(searchEmail);

            // Limiter à 10 résultats max pour éviter surcharge
            int limit = Math.min(clients.size(), 10);

            for (int i = 0; i < limit; i++) {
                Client client = clients.get(i);
                result.addAll(getPublicCompteInfo(client));
            }

            return result;

        } catch (Exception ex) {
            ex.printStackTrace();
            // Retourner liste vide en cas d'erreur au lieu de lancer exception
            return Collections.emptyList();
        }
    }

    private List<ComptePublicInfo> getPublicCompteInfo(Client client) {
        List<ComptePublicInfo> result = new ArrayList<>();

        try {
            // Récupérer uniquement les comptes ACTIFS
            List<Compte> comptes = compteService.findComptesByClientId(client.getId())
                    .stream()
                    .filter(c -> c.getStatutCompte() == StatutCompte.ACTIF)
                    .collect(Collectors.toList());

            // Mapper vers des infos publiques (sans exposer le solde)
            for (Compte compte : comptes) {
                ComptePublicInfo info = new ComptePublicInfo();
                info.setId(compte.getId());
                info.setNumero(compte.getNumero());
                info.setTypeCompte(compte.getTypeCompte());
                info.setLibelle(compte.getLibelle());
                info.setProprietaireNom(client.getNom() + " " + client.getPrenom());
                info.setProprietaireEmail(client.getEmail());
                result.add(info);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }





    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Compte accountById(@Argument String id) {

        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("L'identifiant du compte ne peut pas être vide.");
            }

            Compte compte = compteService.showCompteById(id.trim())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Compte non trouvé avec l'id : " + id)
                    );

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new AccessDeniedException("Utilisateur non authentifié.");
            }

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a ->
                            "ROLE_AGENT_ADMIN".equals(a.getAuthority())
                                    || "ROLE_SUPER_ADMIN".equals(a.getAuthority())
                                    || "ROLE_ADMIN".equals(a.getAuthority())
                    );


            if (!isAdmin) {
                String principalName = auth.getName(); // souvent l'email
                String ownerEmail = compte.getClient() != null
                        ? compte.getClient().getEmail()
                        : null;

                if (ownerEmail == null || !ownerEmail.equalsIgnoreCase(principalName)) {
                    throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter ce compte.");
                }
            }

            return compte;

        } catch (ResourceNotFoundException | AccessDeniedException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération du compte.", ex);
        }
    }


}
