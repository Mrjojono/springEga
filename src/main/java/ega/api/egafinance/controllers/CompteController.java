package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.service.ClientService;
import ega.api.egafinance.service.CompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CompteService compteService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @QueryMapping
    private List<Compte> comptes() {
        return compteService.showCompte();
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

}
