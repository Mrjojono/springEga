package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.ClientMapper;
import ega.api.egafinance.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @QueryMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Client> clients() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Utilisateur authentifié dans SecurityContext: {}", authentication);
        return clientService.showClient();
    }

    @QueryMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Client client(@Argument String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInUserEmail = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_CLIENT")) {
            Client loggedInClient = clientService.getOneClientByEmail(loggedInUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
            if (!loggedInClient.getId().equals(id)) {
                throw new AccessDeniedException("Vous ne pouvez accéder qu'à vos propres informations !");
            }
        }
        return clientService.getOneClient(id).orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id : " + id));
    }

    @MutationMapping
   // @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Client createClient(@Argument @Valid ClientInput input) {
        Client client = clientMapper.toClient(input);
        return clientService.saveClient(client);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Boolean deleteClient(@Argument String id) {
        return clientService.deleteClient(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Client updateClient(@Argument String id, @Argument("client") @Valid ClientInput input) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInUserEmail = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        //verification pour modification que des ces propres informations
        if (role.equals("ROLE_CLIENT")) {
            Client loggedInClient = clientService.getOneClientByEmail(loggedInUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
            if (!loggedInClient.getId().equals(id)) {
                throw new AccessDeniedException("Vous ne pouvez mettre à jour que vos propres informations !");
            }
        }

        return clientService.updateClient(id, input);
    }
}