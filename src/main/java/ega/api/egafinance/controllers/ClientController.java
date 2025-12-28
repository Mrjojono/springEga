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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private final ClientMapper clientMapper;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Client> clients() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Utilisateur authentifié dans SecurityContext: {}", authentication);
        return clientService.showClient();
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public Client client(@Argument String id) {
        return clientService.getOneClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id : " + id));
    }

    @MutationMapping
    public Client createClient(@Argument @Valid ClientInput input) {
        Client client = clientMapper.toClient(input);
        return clientService.saveClient(client);
    }

    @MutationMapping
    public Boolean deleteClient(@Argument String id) {
        return clientService.deleteClient(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Client updateClient(@Argument String id, @Argument("client") @Valid ClientInput input) {
        return clientService.updateClient(id, input);
    }
}