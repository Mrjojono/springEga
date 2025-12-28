package ega.api.egafinance.repository;

import ega.api.egafinance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findAllByCompteSourceIdAndDateCreationBetween(
            String compteSourceId, LocalDateTime startDate, LocalDateTime endDate);


    List<Transaction> findAllByCompteDestinationIdAndDateCreationBetween(
            String compteDestinationId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.compteSource.id = :compteId OR t.compteDestination.id = :compteId")
    List<Transaction> findAllByCompteSourceIdOrCompteDestinationId(
            @Param("compteId") String compteId1,
            @Param("compteId") String compteId2);

}
