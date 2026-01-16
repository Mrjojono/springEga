    package ega.api.egafinance.repository;

    import ega.api.egafinance.entity.ActivationToken;
    import ega.api.egafinance.entity.User;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.Optional;

    public interface ActivationTokenRepository extends JpaRepository<ActivationToken, String> {
        void deleteByUserId(String userId);
        Optional<ActivationToken> findByToken(String token);
    }
