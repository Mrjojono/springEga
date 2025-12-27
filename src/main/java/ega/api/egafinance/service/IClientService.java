package ega.api.egafinance.service;


import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.entity.Client;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

public interface IClientService {

    //afficher tous les clients de la base de données
    public List<Client> showClient();

    //enregistrer un client dans la base de données
    public Client saveClient( Client client);

    //récupérer un client par id
    public Optional<Client> getOneClient(String  id);

    //supprimer un client
    public Boolean deleteClient(String id);

    //mettre a jour un client
    @Transactional
    Client updateClient(String id, ClientInput clientInput);
}
