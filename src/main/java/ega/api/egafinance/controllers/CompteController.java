package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.dto.CompteUpdateInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
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
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
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
    public Compte updateCompte(@Argument String id, @Argument("compte") @Valid CompteUpdateInput input) {
        return compteService.updateCompte(id, input);
    }
}
