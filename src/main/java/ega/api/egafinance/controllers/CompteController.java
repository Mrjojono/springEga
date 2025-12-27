package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.dto.CompteUpdateInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.service.ClientService;
import ega.api.egafinance.service.CompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CompteController {


    private final CompteService compteService;
    private final ClientService clientService;
    private final ClientRepository clientRepository;


    @QueryMapping
    public List<Compte> comptes(@Argument Integer page, @Argument Integer size) {
        int pageIndex = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return compteService.getPagedCompte(pageable);
    }

    @MutationMapping
    public Compte createCompte(@Argument("input") @Valid CompteInput compteInput) {
        return compteService.saveCompte(compteInput);
    }

    @SchemaMapping(typeName = "Compte", field = "proprietaire")
    public Client proprietaire(Compte compte) {
        return compte.getClient();
    }

    @MutationMapping
    public  Boolean deleteCompte(@Argument String id){
        return  compteService.deleteCompte(id);
    }

    @MutationMapping
    public Compte updateCompte(@Argument String id, @Argument("compte") @Valid CompteUpdateInput input) {
        return compteService.updateCompte(id,input);
    }
}
