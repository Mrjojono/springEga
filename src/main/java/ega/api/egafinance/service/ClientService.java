package ega.api.egafinance.service;


import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.ClientMapper;
import ega.api.egafinance.repository.ClientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private final ClientMapper clientMapper;

    @Autowired
    private EmailService emailService;


    public Optional<Client> getClient(String id) {
        return clientRepository.findById(id);
    }

    public Optional<Client> getOneClientByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    @Override
    public List<Client> showClient() {
        return clientRepository.findAll();
    }


    /**
     * @param client
     * @return
     */
    @Override
    @Transactional
    public Client saveClient(Client client) {
        Client savedClient = clientRepository.save(client);
        // Envoyer l'email après la sauvegarde avec l'identifiant unique
        emailService.sendWelcomeEmail(savedClient.getEmail(), savedClient.getPrenom(), savedClient.getIdentifiant());
        return savedClient;
    }

    /**
     * @param id
     * @return Client
     */
    @Override
    public Optional<Client> getOneClient(String id) {
        return clientRepository.findById(id);
    }

    /**
     * @param id
     * @return True/False
     */
    @Override
    @Transactional
    public Boolean deleteClient(String id) {
        try {
            if (!clientRepository.existsById(id)) {
                return false;
            }
            clientRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Transactional
    @Override
    public Client updateClient(String id, ClientInput clientInput) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client non trouvé avec l'id : " + id
                ));

        clientMapper.updateClientFromInput(clientInput, existingClient);
        return clientRepository.save(existingClient);
    }


}
