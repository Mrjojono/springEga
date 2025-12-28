package ega.api.egafinance.repository;

import ega.api.egafinance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findAllByCompteSourceIdAndDateCreationBetween(
            String compteSourceId, LocalDateTime startDate, LocalDateTime endDate);


    List<Transaction> findAllByCompteDestinationIdAndDateCreationBetween(
            String compteDestinationId, LocalDateTime startDate, LocalDateTime endDate);
}
