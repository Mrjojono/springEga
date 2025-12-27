package ega.api.egafinance.service;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.CompteMapper;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.repository.CompteRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompteService implements ICompteService {


    private final CompteMapper compteMapper;
    private final CompteRepository compteRepository;
    private final ClientRepository clientRepository;


    /**
     * @return List<Compte>
     */
    @Override
    public List<Compte> showCompte() {
        return compteRepository.findAll();
    }


    /**
     * @param compteInput
     * @return
     */
    @Override
    @Transactional
    public Compte saveCompte(CompteInput compteInput) {
        Compte compte = compteMapper.toCompte(compteInput);


        if (compteInput.getProprietaireId() != null) {
            Client client = clientRepository.findById(compteInput.getProprietaireId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client non trouvé avec l'id : " + compteInput.getProprietaireId()
                    ));
            compte.setClient(client);
        }

        compte.setDateCreation(LocalDateTime.now());
        return compteRepository.save(compte);
    }

    /**
     * @param id
     * @return
     */
    @Override
    @Transactional
    public Boolean deleteCompte(String id) {
        try {
            if (!compteRepository.existsById(id)) {
                return false;
            }
            compteRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * @param id
     * @param compteInput
     * @return
     */
    @Override
    public Compte updateCompte(String id, CompteInput compteInput) {
        Compte existingCompte = compteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "Compte non trouvé avec l'id : " + id
        ));
        compteMapper.updateCompteFromInput(compteInput, existingCompte);
        return compteRepository.save(existingCompte);
    }


}
