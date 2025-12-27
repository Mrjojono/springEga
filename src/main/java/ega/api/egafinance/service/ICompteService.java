package ega.api.egafinance.service;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.entity.Compte;

import java.util.List;

public interface ICompteService {


    //creation d'un compte
    public Compte saveCompte(CompteInput compteInput);

    //suppression d'un compte
    public Boolean deleteCompte(String id);

    //modifier un compte
    public Compte updateCompte(String id, CompteInput compteInput);

    //recuperer tous les comptes
    public List<Compte> showCompte();

}
