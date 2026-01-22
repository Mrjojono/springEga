package ega.api.egafinance.service;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.dto.CompteUpdateInput;
import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.StatutCompte;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.CompteMapper;
import ega.api.egafinance.repository.ClientRepository;
import ega.api.egafinance.repository.CompteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.iban4j.CountryCode;
import org.iban4j.Iban;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class CompteService implements ICompteService {


    private final CompteMapper compteMapper;
    private final CompteRepository compteRepository;
    private final ClientRepository clientRepository;
    private final TransactionService transactionService;


    /**
     * @return List<Compte>
     */
    @Override
    public List<Compte> showCompte() {
        return compteRepository.findAll();
    }

    /**
     * Retourne la liste des comptes d'un client identifié par son id.
     */
    public List<Compte> findComptesByClientId(String clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id : " + clientId));
        return compteRepository.findByClientId(clientId);
    }


    public Optional<Compte> showCompteById(String id) {
        return compteRepository.findById(id);
    }

    public List<Compte> getPagedCompte(Pageable pageable) {
        Page<Compte> comptePage = compteRepository.findAll(pageable);
        return comptePage.getContent();
    }

    /**
     * @param compteInput
     * @return
     */
    @Override
    @Transactional
    public Compte saveCompte(CompteInput compteInput) {
        Compte compte = compteMapper.toCompte(compteInput);

        Client client = clientRepository.findById(compteInput.getProprietaireId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client non trouvé avec l'id : " + compteInput.getProprietaireId()
                ));
        compte.setClient(client);

        // Générer un numéro IBAN unique (avec retry en cas de doublon)
        String numeroGenere = null;
        int maxAttempts = 10;
        int attempts = 0;

        while (attempts < maxAttempts) {
            numeroGenere = generateNumeroCompte(client);

            // Vérifier si le numéro existe déjà
            if (!compteRepository.existsByNumero(numeroGenere)) {
                break; // Numéro unique trouvé
            }

            attempts++;
        }

        if (attempts >= maxAttempts) {
            throw new RuntimeException("Impossible de générer un numéro de compte unique après " + maxAttempts + " tentatives");
        }

        compte.setNumero(numeroGenere);

        if (compte.getSolde() == null) {
            compte.setSolde(BigDecimal.ZERO);
        }

        compte.setStatutCompte(StatutCompte.ACTIF);
        return compteRepository.save(compte);
    }


    /**
     * @param id
     * @return Boolean
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
     * @param compteUpdateInput
     * @return
     */
    @Override
    @Transactional
    public Compte updateCompte(String id, CompteUpdateInput compteUpdateInput) {
        Compte existingCompte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte not found with ID: " + id
                ));

        if (compteUpdateInput.getNumero() != null) {
            existingCompte.setNumero(compteUpdateInput.getNumero());
        }
        if (compteUpdateInput.getSolde() != null) {
            existingCompte.setSolde(compteUpdateInput.getSolde());
        }
        if (compteUpdateInput.getTypeCompte() != null) {
            existingCompte.setTypeCompte(compteUpdateInput.getTypeCompte());
        }
        if (compteUpdateInput.getProprietaireId() != null) {
            Client proprietaire = clientRepository.findById(compteUpdateInput.getProprietaireId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client not found with ID: " + compteUpdateInput.getProprietaireId()
                    ));
            existingCompte.setClient(proprietaire);
        }

        return compteRepository.save(existingCompte);
    }



    /**
     * Génère un numéro IBAN valide pour un client
     * Format international simplifié avec validation
     */
    private String generateNumeroCompte(Client client) {
        SecureRandom random = new SecureRandom();

        // Utiliser toujours le code pays FR pour simplifier
        CountryCode countryCode = CountryCode.FR;

        try {
            // Format français : FR + 2 chiffres contrôle + 23 caractères BBAN
            // BBAN = 5 (banque) + 5 (guichet) + 11 (compte) + 2 (clé)
            String bankCode = String.format("%05d", 10000 + random.nextInt(90000));
            String branchCode = String.format("%05d", 10000 + random.nextInt(90000));
            String accountNumber = String.format("%011d",
                    10000000000L + Math.abs(random.nextLong() % 90000000000L));
            String ribKey = String.format("%02d", random.nextInt(97));

            // BBAN complet = 23 caractères
            String bban = bankCode + branchCode + accountNumber + ribKey;

            // Construire l'IBAN avec iban4j
            Iban iban = new Iban.Builder()
                    .countryCode(countryCode)
                    .bankCode(bankCode)
                    .branchCode(branchCode)
                    .accountNumber(accountNumber + ribKey)
                    .build();

            return iban.toString();
        } catch (Exception e) {
            // En cas d'erreur, générer manuellement
            return generateManualIban(random);
        }
    }

    /**
     * Génère un IBAN manuellement sans iban4j (fallback)
     */
    private String generateManualIban(SecureRandom random) {
        // Format: FR + 2 chiffres contrôle + 23 chiffres BBAN
        String bankCode = String.format("%05d", 10000 + random.nextInt(90000));
        String branchCode = String.format("%05d", 10000 + random.nextInt(90000));
        String accountNumber = String.format("%011d",
                10000000000L + Math.abs(random.nextLong() % 90000000000L));
        String ribKey = String.format("%02d", random.nextInt(97));

        String bban = bankCode + branchCode + accountNumber + ribKey;

        // Calculer les chiffres de contrôle
        String checkDigits = calculateIbanCheckDigits("FR", bban);

        return "FR" + checkDigits + bban;
    }

    /**
     * Calcule les 2 chiffres de contrôle de l'IBAN (modulo 97)
     */
    private String calculateIbanCheckDigits(String countryCode, String bban) {
        // Déplacer le code pays et "00" à la fin
        String rearranged = bban + countryCode + "00";

        // Convertir les lettres en chiffres (A=10, B=11, ..., Z=35)
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.getNumericValue(c));
            } else {
                numeric.append(c);
            }
        }

        // Calculer modulo 97
        java.math.BigInteger mod = new java.math.BigInteger(numeric.toString())
                .mod(java.math.BigInteger.valueOf(97));
        int checkDigits = 98 - mod.intValue();

        return String.format("%02d", checkDigits);
    }

    /**
     * Détermine le CountryCode basé sur la nationalité du client
     */
    private CountryCode determineCountryCode(String nationalite) {
        if (nationalite == null || nationalite.length() < 2) {
            return CountryCode.FR; // Par défaut France
        }

        String code = nationalite.substring(0, 2).toUpperCase();

        try {
            // Mapping des codes pays courants
            return switch (code) {
                case "FR" -> CountryCode.FR;
                case "SN" -> CountryCode.FR; // Sénégal n'a pas d'IBAN, utiliser FR
                case "BE" -> CountryCode.BE;
                case "CH" -> CountryCode.CH;
                case "DE" -> CountryCode.DE;
                case "ES" -> CountryCode.ES;
                case "IT" -> CountryCode.IT;
                case "GB" -> CountryCode.GB;
                case "NL" -> CountryCode.NL;
                case "LU" -> CountryCode.LU;
                case "PT" -> CountryCode.PT;
                case "CI" -> CountryCode.FR; // Côte d'Ivoire, utiliser FR
                case "MA" -> CountryCode.FR; // Maroc, utiliser FR
                case "TN" -> CountryCode.FR; // Tunisie, utiliser FR
                default -> CountryCode.FR;
            };
        } catch (Exception e) {
            return CountryCode.FR;
        }
    }

    /**
     * Génère un code banque selon le pays
     */
    private String generateBankCode(CountryCode country, SecureRandom random) {
        return switch (country) {
            case FR -> String.format("%05d", 10000 + random.nextInt(90000)); // 5 chiffres
            case BE -> String.format("%03d", 100 + random.nextInt(900)); // 3 chiffres
            case DE -> String.format("%08d", 10000000 + random.nextInt(90000000)); // 8 chiffres
            case ES -> String.format("%04d", 1000 + random.nextInt(9000)); // 4 chiffres
            case IT -> String.format("%05d", 10000 + random.nextInt(90000)); // 5 chiffres
            case GB -> "BUKB"; // Code bancaire UK (4 caractères alphanumériques)
            case CH -> String.format("%05d", 10000 + random.nextInt(90000)); // 5 chiffres
            default -> String.format("%05d", 10000 + random.nextInt(90000));
        };
    }

    /**
     * Génère un code guichet selon le pays
     */
    private String generateBranchCode(CountryCode country, SecureRandom random) {
        return switch (country) {
            case FR -> String.format("%05d", 10000 + random.nextInt(90000)); // 5 chiffres
            case BE -> ""; // Pas de branch code en Belgique
            case DE -> ""; // Pas de branch code séparé en Allemagne
            case ES -> String.format("%04d", 1000 + random.nextInt(9000)); // 4 chiffres
            case IT -> String.format("%05d", 10000 + random.nextInt(90000)); // 5 chiffres
            case GB -> String.format("%06d", 100000 + random.nextInt(900000)); // 6 chiffres
            case CH -> ""; // Pas de branch code en Suisse
            default -> String.format("%05d", 10000 + random.nextInt(90000));
        };
    }

    /**
     * Génère un numéro de compte selon le pays
     */
    private String generateAccountNumber(CountryCode country, SecureRandom random) {
        return switch (country) {
            // France: BBAN = 5 (bank) + 5 (branch) + 11 (account) + 2 (key) = 23
            case FR -> String.format("%011d%02d",
                    10000000000L + Math.abs(random.nextLong() % 90000000000L),
                    random.nextInt(97)); // 11 chiffres + 2 chiffres clé RIB
            // Belgique: BBAN = 3 (bank) + 7 (account) + 2 (check) = 12
            case BE -> String.format("%07d%02d",
                    1000000 + random.nextInt(9000000),
                    random.nextInt(97));
            // Allemagne: BBAN = 8 (bank) + 10 (account) = 18
            case DE -> String.format("%010d",
                    1000000000L + Math.abs(random.nextLong() % 9000000000L));
            // Espagne: BBAN = 4 (bank) + 4 (branch) + 2 (check) + 10 (account) = 20
            case ES -> String.format("%02d%010d",
                    random.nextInt(97),
                    1000000000L + Math.abs(random.nextLong() % 9000000000L));
            // Italie: BBAN = 1 (check) + 5 (bank) + 5 (branch) + 12 (account) = 23
            case IT -> String.format("%c%012d",
                    'A' + random.nextInt(26),
                    100000000000L + Math.abs(random.nextLong() % 900000000000L));
            // UK: BBAN = 4 (bank) + 6 (branch) + 8 (account) = 18
            case GB -> String.format("%08d",
                    10000000 + random.nextInt(90000000));
            // Suisse: BBAN = 5 (bank) + 12 (account) = 17
            case CH -> String.format("%012d",
                    100000000000L + Math.abs(random.nextLong() % 900000000000L));
            default -> String.format("%011d%02d",
                    10000000000L + Math.abs(random.nextLong() % 90000000000L),
                    random.nextInt(97));
        };
    }

    /**
     * Génère un IBAN français de secours en cas d'erreur
     */
    private String generateFallbackIban(SecureRandom random) {
        return new Iban.Builder()
                .countryCode(CountryCode.FR)
                .bankCode(String.format("%05d", 10000 + random.nextInt(90000)))
                .branchCode(String.format("%05d", 10000 + random.nextInt(90000)))
                .accountNumber(String.format("%011d%02d",
                        10000000000L + Math.abs(random.nextLong() % 90000000000L),
                        random.nextInt(97)))
                .build()
                .toString();
    }
}
