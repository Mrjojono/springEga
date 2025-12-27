package ega.api.egafinance.service;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.TransactionMapper;
import ega.api.egafinance.repository.CompteRepository;
import ega.api.egafinance.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService implements ITransactionService {

    private final CompteRepository compteRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> getPagedTransactions(Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        return transactionPage.getContent();
    }

    @Transactional
    public Transaction versement(TransactionInput transactionInput) {
        BigDecimal montant = transactionInput.getMontant();
        String idCompteDest = transactionInput.getCompte_destination_Id();
        String idCompteSource = transactionInput.getCompte_source_Id();


        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro !");
        }


        Compte compteDest = compteRepository.findById(idCompteDest)
                .orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));


        Compte compteSource = null;
        if (idCompteSource != null) {
            compteSource = compteRepository.findById(idCompteSource)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte source introuvable avec l'ID : " + idCompteSource));

            // Vérifiez que le compte source dispose des fonds nécessaires
            if (compteSource.getSolde().compareTo(montant) < 0) {
                throw new IllegalArgumentException("Fonds insuffisants sur le compte source !");
            }

            // Déduire le montant du compte source
            compteSource.setSolde(compteSource.getSolde().subtract(montant));
            compteRepository.save(compteSource);
        }


        compteDest.setSolde(compteDest.getSolde().add(montant));
        compteRepository.save(compteDest);


        Transaction transaction = transactionMapper.toTransaction(transactionInput);
        transaction.setMontant(montant);
        transaction.setCompteSource(compteSource);
        transaction.setCompteDestination(compteDest);
        transaction.setDateCreation(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }
}