package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.ClientMapper;
import ega.api.egafinance.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private final ClientMapper clientMapper;

    @QueryMapping
    public List<Client> clients() {
        return clientService.showClient();
    }

    @QueryMapping
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
    public Client updateClient(@Argument String id, @Argument("client") @Valid ClientInput input) {
        return clientService.updateClient(id, input);
    }
}