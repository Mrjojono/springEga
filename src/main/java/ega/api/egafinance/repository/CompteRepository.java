package ega.api.egafinance.repository;

import ega.api.egafinance.entity.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  CompteRepository extends JpaRepository<Compte,String> {
    Optional<Object> getCompteById(String id);
}
